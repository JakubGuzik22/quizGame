package com.example.quizgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizgame.databinding.ActivityLobbyBinding
import com.example.quizgame.databinding.ItemPlayerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class LobbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLobbyBinding
    private var isHost = false
    private var roomName = ""
    private var playerName = ""
    private var questionCount = 10
    private var port = 6000

    private val players = mutableListOf<Player>()
    private lateinit var adapter: PlayerAdapter

    private var serverSocket: ServerSocket? = null
    private val clientStreams = mutableListOf<ObjectOutputStream>()
    private val clientSockets = mutableListOf<Socket>()
    private var clientSocket: Socket? = null
    @Volatile private var isRunning = true
    @Volatile private var isStartingGame = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isHost = intent.getBooleanExtra("IS_HOST", false)
        roomName = intent.getStringExtra("ROOM_NAME") ?: "Pokój gier"
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Gracz"
        port = intent.getIntExtra("PORT", 6000)

        setupUI()
        setupRecyclerView()
        
        if (isHost) {
            synchronized(players) {
                players.add(Player(playerName, true))
            }
            startServer()
        } else {
            val hostAddress = intent.getStringExtra("HOST_ADDRESS")
            connectToHost(hostAddress)
        }
        updatePlayerCount()
    }

    private fun setupUI() {
        binding.tvRoomName.text = "Pokój: $roomName"
        binding.llHostSettings.isVisible = isHost
        binding.btnStartGame.isVisible = isHost

        if (isHost) {
            binding.btnChangeQuestions.setOnClickListener { showQuestionCountDialog() }
            binding.btnStartGame.setOnClickListener { startGame() }
        }

        binding.btnLeaveLobby.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PlayerAdapter(players)
        binding.rvPlayers.layoutManager = LinearLayoutManager(this)
        binding.rvPlayers.adapter = adapter
    }

    private fun updatePlayerCount() {
        runOnUiThread {
            binding.tvPlayerCount.text = "Gracze (${players.size}/4):"
            adapter.notifyDataSetChanged()
        }
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket()
                serverSocket?.reuseAddress = true
                serverSocket?.bind(InetSocketAddress(port))
                while (isRunning) {
                    val socket = try { serverSocket?.accept() } catch (e: Exception) { null } ?: break
                    if (players.size >= 4) {
                        socket.close()
                        continue
                    }
                    handleNewClient(socket)
                }
            } catch (e: Exception) {
                if (isRunning) Log.e("Lobby", "Server error", e)
            }
        }.start()
    }

    private fun handleNewClient(socket: Socket) {
        Thread {
            try {
                val out = ObjectOutputStream(socket.getOutputStream())
                val `in` = ObjectInputStream(socket.getInputStream())
                
                val clientName = `in`.readUTF()
                val newPlayer = Player(clientName, false)
                
                synchronized(players) {
                    players.add(newPlayer)
                    clientStreams.add(out)
                    clientSockets.add(socket)
                }
                
                updatePlayerCount()
                broadcastState()

                try {
                    while (isRunning) {
                        `in`.read() 
                    }
                } catch (e: Exception) {
                } finally {
                    if (!isStartingGame) {
                        synchronized(players) {
                            players.remove(newPlayer)
                            clientStreams.remove(out)
                            clientSockets.remove(socket)
                        }
                        updatePlayerCount()
                        broadcastState()
                    }
                    try { socket.close() } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                if (isRunning && !isStartingGame) {
                    if (e is EOFException || e is SocketException) {
                        Log.i("Lobby", "Client disconnected during handshake")
                    } else {
                        Log.e("Lobby", "Client handler error", e)
                    }
                }
            }
        }.start()
    }

    private fun connectToHost(address: String?) {
        Thread {
            try {
                clientSocket = Socket(address, port)
                val out = ObjectOutputStream(clientSocket?.getOutputStream())
                val `in` = ObjectInputStream(clientSocket?.getInputStream())
                
                out.writeUTF(playerName)
                out.flush()
                
                while (isRunning) {
                    try {
                        val data = `in`.readObject()
                        if (data is List<*>) {
                            synchronized(players) {
                                players.clear()
                                @Suppress("UNCHECKED_CAST")
                                players.addAll(data as List<Player>)
                            }
                            updatePlayerCount()
                        } else if (data is String && data.startsWith("START:")) {
                            val parts = data.split(":")
                            val count = parts[1].toInt()
                            val pCount = if (parts.size > 2) parts[2].toInt() else players.size
                            isStartingGame = true
                            runOnUiThread { goToQuiz(count, pCount) }
                            break
                        }
                    } catch (e: Exception) {
                        if (isRunning && !isStartingGame) {
                            if (e is EOFException || e is SocketException) {
                                Log.i("Lobby", "Host disconnected")
                            } else {
                                Log.e("Lobby", "Read error", e)
                            }
                            runOnUiThread {
                                Toast.makeText(this@LobbyActivity, "Połączenie z hostem przerwane", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    runOnUiThread {
                        Toast.makeText(this, "Nie można połączyć się z hostem", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }.start()
    }

    private fun broadcastState() {
        synchronized(players) {
            val playersList = ArrayList(players)
            for (out in clientStreams) {
                try {
                    out.reset()
                    out.writeObject(playersList)
                    out.flush()
                } catch (e: Exception) {}
            }
        }
    }

    private fun showQuestionCountDialog() {
        val options = arrayOf("10 pytań", "20 pytań", "30 pytań")
        val values = intArrayOf(10, 20, 30)
        MaterialAlertDialogBuilder(this)
            .setTitle("Wybierz liczbę pytań")
            .setItems(options) { _, which ->
                questionCount = values[which]
                binding.tvQuestionCount.text = "Liczba pytań: $questionCount"
            }
            .show()
    }

    private fun startGame() {
        val playerCount = synchronized(players) { players.size }
        isStartingGame = true
        Thread {
            synchronized(players) {
                for (out in clientStreams) {
                    try {
                        out.reset()
                        out.writeObject("START:$questionCount:$playerCount")
                        out.flush()
                    } catch (e: Exception) {
                        Log.e("Lobby", "Error sending start signal", e)
                    }
                }
            }
            runOnUiThread {
                // Krótka pauza, aby upewnić się, że pakiety zostały wysłane
                binding.root.postDelayed({
                    goToQuiz(questionCount, playerCount)
                }, 500)
            }
        }.start()
    }

    private fun goToQuiz(count: Int, playerCount: Int) {
        isRunning = false
        Thread {
            try {
                serverSocket?.close()
                clientSocket?.close()
                synchronized(players) {
                    clientSockets.forEach { it.close() }
                    clientSockets.clear()
                    clientStreams.clear()
                }
                Thread.sleep(200)
            } catch (e: Exception) {}
            runOnUiThread {
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("IS_HOST", isHost)
                    putExtra("QUESTION_COUNT", count)
                    putExtra("PORT", port)
                    putExtra("PLAYER_NAME", playerName)
                    putExtra("PLAYER_COUNT", playerCount)
                    putExtra("HOST_ADDRESS", this@LobbyActivity.intent.getStringExtra("HOST_ADDRESS"))
                }
                startActivity(intent)
                finish()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Thread {
            try {
                serverSocket?.close()
                clientSocket?.close()
                synchronized(players) {
                    clientSockets.forEach { it.close() }
                    clientSockets.clear()
                    clientStreams.clear()
                }
            } catch (e: Exception) {}
        }.start()
    }

    data class Player(val name: String, val isHost: Boolean) : Serializable

    inner class PlayerAdapter(private val playerList: List<Player>) :
        RecyclerView.Adapter<PlayerAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemPlayerBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(player: Player) {
                itemBinding.tvPlayerName.text = player.name
                itemBinding.tvHostBadge.isVisible = player.isHost
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(playerList[position])
        }

        override fun getItemCount() = playerList.size
    }
}
