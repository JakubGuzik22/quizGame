package com.example.quizgame

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.quizgame.databinding.ActivityQuizBinding
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.ArrayList

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private lateinit var repository: QuizRepository
    private lateinit var questionsList: List<Question>
    private var mediaPlayer: MediaPlayer? = null

    private var currentQuestionIndex = 0
    private var score = 0
    private var totalQuestionsToAsk = 10
    private var progressColors = ArrayList<Int>()

    private var isMultiplayer = false
    private var isHost = false
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var objectIn: ObjectInputStream? = null
    private var objectOut: ObjectOutputStream? = null

    private lateinit var answerButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("DARK_MODE", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalQuestionsToAsk = intent.getIntExtra("QUESTION_COUNT", 10)
        isHost = intent.getBooleanExtra("IS_HOST", false)
        val hostAddress = intent.getStringExtra("HOST_ADDRESS")
        val port = intent.getIntExtra("PORT", -1)
        isMultiplayer = port != -1

        repository = QuizRepository(this)
        
        answerButtons = listOf(
            binding.btnAnswer1,
            binding.btnAnswer2,
            binding.btnAnswer3,
            binding.btnAnswer4
        )

        if (isMultiplayer) {
            startMultiplayer(port, hostAddress)
        } else {
            if (savedInstanceState != null) {
                currentQuestionIndex = savedInstanceState.getInt("CURRENT_INDEX")
                score = savedInstanceState.getInt("SCORE")
                @Suppress("DEPRECATION", "UNCHECKED_CAST")
                questionsList = savedInstanceState.getSerializable("QUESTIONS_LIST") as ArrayList<Question>
                progressColors = savedInstanceState.getIntegerArrayList("PROGRESS_COLORS") ?: ArrayList()
            } else {
                val allQuestions = repository.getAllQuestions().shuffled()
                questionsList = allQuestions.take(totalQuestionsToAsk)
            }
            setupProgressBar()
            displayQuestion()
        }
    }

    private fun startMultiplayer(port: Int, hostAddress: String?) {
        binding.flWaitingOverlay.visibility = View.VISIBLE
        
        Thread {
            try {
                if (isHost) {
                    serverSocket = ServerSocket(port)
                    socket = serverSocket?.accept()
                } else {
                    var connected = false
                    var attempts = 0
                    // Add a small initial delay to give the host time to start the ServerSocket
                    Thread.sleep(500)
                    while (!connected && attempts < 10) {
                        try {
                            val s = Socket()
                            s.connect(InetSocketAddress(hostAddress, port), 5000)
                            socket = s
                            connected = true
                        } catch (e: Exception) {
                            attempts++
                            if (attempts >= 10) throw e
                            Thread.sleep(1000)
                        }
                    }
                }

                objectOut = ObjectOutputStream(socket?.getOutputStream())
                objectIn = ObjectInputStream(socket?.getInputStream())

                if (isHost) {
                    val allQuestions = repository.getAllQuestions().shuffled()
                    questionsList = allQuestions.take(totalQuestionsToAsk)
                    objectOut?.writeObject(questionsList)
                    objectOut?.flush()
                } else {
                    @Suppress("UNCHECKED_CAST")
                    questionsList = objectIn?.readObject() as ArrayList<Question>
                }

                runOnUiThread {
                    binding.flWaitingOverlay.visibility = View.GONE
                    setupProgressBar()
                    displayQuestion()
                }
            } catch (e: Exception) {
                Log.e("Multiplayer", "Connection error", e)
                runOnUiThread {
                    Toast.makeText(this, "Błąd połączenia: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }

    private fun setupProgressBar() {
        binding.llProgressBar.removeAllViews()
        for (i in questionsList.indices) {
            val segment = View(this)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            if (i < questionsList.size - 1) {
                params.marginEnd = 4
            }
            segment.layoutParams = params
            
            val color = if (i < progressColors.size) {
                progressColors[i]
            } else {
                ContextCompat.getColor(this, android.R.color.darker_gray)
            }
            segment.setBackgroundColor(color)
            binding.llProgressBar.addView(segment)
        }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex >= questionsList.size) {
            if (isMultiplayer) {
                syncScoreAndEnd()
            } else {
                endQuiz()
            }
            return
        }

        val currentQuestion = questionsList[currentQuestionIndex]
        val correctAnswer = repository.getAuthorName(currentQuestion.authorId)
        
        binding.tvProgress.text = "Pytanie: ${currentQuestionIndex + 1} / ${questionsList.size}"
        binding.tvQuestion.text = "\"${currentQuestion.question}\""
        binding.ivQuestionImage.setImageResource(R.drawable.quiz_icon)

        val generatedAnswers = repository.generateAnswersForQuestion(currentQuestion)

        for (i in answerButtons.indices) {
            val button = answerButtons[i]
            val answerText = generatedAnswers[i]
            
            button.text = answerText
            button.isEnabled = true
            // Reset to default color (using a common color or null to revert to theme)
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.black)) 
            button.setTextColor(Color.WHITE)

            button.setOnClickListener {
                checkAnswer(button, answerText, correctAnswer)
            }
        }
    }

    private fun checkAnswer(selectedButton: Button, selectedAnswer: String, correctAnswer: String) {
        // Disable all buttons to prevent multiple clicks
        answerButtons.forEach { it.isEnabled = false }

        val isCorrect = selectedAnswer == correctAnswer

        if (isCorrect) {
            score++
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            progressColors.add(ContextCompat.getColor(this, R.color.green))
            playSound(R.raw.answer_correct)
        } else {
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            progressColors.add(ContextCompat.getColor(this, R.color.red))
            playSound(R.raw.answer_incorrect)
            // Highlight the correct answer
            answerButtons.forEach { button ->
                if (button.text == correctAnswer) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                }
            }
        }
        
        revealAuthorImage()
        updateProgressBar()

        // Wait 3 seconds before next question
        Handler(Looper.getMainLooper()).postDelayed({
            currentQuestionIndex++
            displayQuestion()
        }, 3000)
    }

    private fun updateProgressBar() {
        val segment = binding.llProgressBar.getChildAt(currentQuestionIndex)
        segment?.setBackgroundColor(progressColors.last())
    }

    private fun revealAuthorImage() {
        val currentQuestion = questionsList[currentQuestionIndex]
        val imageName = repository.getAuthorImage(currentQuestion.authorId)
        if (imageName != null) {
            val resId = resources.getIdentifier(imageName, "drawable", packageName)
            if (resId != 0) {
                binding.ivQuestionImage.setImageResource(resId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_INDEX", currentQuestionIndex)
        outState.putInt("SCORE", score)
        outState.putSerializable("QUESTIONS_LIST", ArrayList(questionsList))
        outState.putIntegerArrayList("PROGRESS_COLORS", progressColors)
    }

    private fun syncScoreAndEnd() {
        binding.flWaitingOverlay.visibility = View.VISIBLE
        binding.tvWaitingMessage.text = "Oczekiwanie na wynik przeciwnika..."
        
        Thread {
            try {
                objectOut?.writeInt(score)
                objectOut?.flush()
                
                val opponentScore = objectIn?.readInt() ?: 0
                
                runOnUiThread {
                    val intent = Intent(this, SummaryActivity::class.java).apply {
                        putExtra("SCORE", score)
                        putExtra("TOTAL_QUESTIONS", questionsList.size)
                        putIntegerArrayListExtra("PROGRESS_COLORS", progressColors)
                        putExtra("OPPONENT_SCORE", opponentScore)
                        putExtra("IS_MULTIPLAYER", true)
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    endQuiz() // Fallback to normal end if connection fails
                }
            }
        }.start()
    }

    private fun endQuiz() {
        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL_QUESTIONS", questionsList.size)
            putIntegerArrayListExtra("PROGRESS_COLORS", progressColors)
        }
        startActivity(intent)
        finish()
    }

    private fun playSound(resId: Int) {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val isSoundEnabled = sharedPref.getBoolean("SOUND_ENABLED", true)
        if (!isSoundEnabled) return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            objectIn?.close()
            objectOut?.close()
            socket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}
