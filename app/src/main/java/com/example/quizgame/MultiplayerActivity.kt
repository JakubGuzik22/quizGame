package com.example.quizgame

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizgame.databinding.ActivityMultiplayerBinding
import java.net.ServerSocket

class MultiplayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMultiplayerBinding
    private lateinit var nsdManager: NsdManager
    private val SERVICE_TYPE = "_quizgame._tcp."
    private val SERVICE_NAME = "QuizGameHost"

    private val discoveredServices = mutableListOf<NsdServiceInfo>()
    private lateinit var adapter: RoomAdapter

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        setupRecyclerView()

        binding.btnHost.setOnClickListener {
            startHosting()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        startDiscovery()
    }

    private fun setupRecyclerView() {
        adapter = RoomAdapter(discoveredServices) { serviceInfo ->
            resolveAndJoin(serviceInfo)
        }
        binding.rvRooms.layoutManager = LinearLayoutManager(this)
        binding.rvRooms.adapter = adapter
    }

    private fun startHosting() {
        val localPort = 6000//findFreePort()
        registerService(localPort)
        
        // In a real app, we would start a ServerSocket here and wait for connection
        // Then start MultiplayerQuizActivity as Host
        Toast.makeText(this, "Hostowanie na porcie $localPort...", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("IS_HOST", true)
            putExtra("PORT", localPort)
            putExtra("QUESTION_COUNT", 10) // Default
        }
        startActivity(intent)
    }

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d("NSD", "Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NSD", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "Discovery start failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "Discovery stop failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NSD", "Discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NSD", "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NSD", "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == SERVICE_TYPE) {
                    if (serviceInfo.serviceName.contains(SERVICE_NAME)) {
                        runOnUiThread {
                            if (!discoveredServices.any { it.serviceName == serviceInfo.serviceName }) {
                                discoveredServices.add(serviceInfo)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NSD", "Service lost: ${serviceInfo.serviceName}")
                runOnUiThread {
                    discoveredServices.removeAll { it.serviceName == serviceInfo.serviceName }
                    adapter.notifyDataSetChanged()
                }
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveAndJoin(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NSD", "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                Log.d("NSD", "Resolve Succeeded: $resolvedServiceInfo")
                val host = resolvedServiceInfo.host.hostAddress
                val port = resolvedServiceInfo.port
                
                runOnUiThread {
                    Toast.makeText(this@MultiplayerActivity, "Łączenie z $host:$port", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@MultiplayerActivity, QuizActivity::class.java).apply {
                        putExtra("IS_HOST", false)
                        putExtra("HOST_ADDRESS", host)
                        putExtra("PORT", port)
                    }
                    startActivity(intent)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        registrationListener?.let { nsdManager.unregisterService(it) }
        discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
    }

    inner class RoomAdapter(
        private val rooms: List<NsdServiceInfo>,
        private val onClick: (NsdServiceInfo) -> Unit
    ) : RecyclerView.Adapter<RoomAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val room = rooms[position]
            holder.tvName.text = room.serviceName
            holder.itemView.setOnClickListener { onClick(room) }
        }

        override fun getItemCount() = rooms.size
    }
}
