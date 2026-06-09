package com.example.quizgame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.quizgame.databinding.ActivityQuizBinding
import java.util.ArrayList

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private lateinit var repository: QuizRepository
    private lateinit var questionsList: List<Question>

    private var currentQuestionIndex = 0
    private var score = 0
    private var totalQuestionsToAsk = 10
    private var progressColors = ArrayList<Int>()

    private lateinit var answerButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalQuestionsToAsk = intent.getIntExtra("QUESTION_COUNT", 10)

        repository = QuizRepository(this)
        
        if (savedInstanceState != null) {
            currentQuestionIndex = savedInstanceState.getInt("CURRENT_INDEX")
            score = savedInstanceState.getInt("SCORE")
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            questionsList = savedInstanceState.getSerializable("QUESTIONS_LIST") as ArrayList<Question>
            progressColors = savedInstanceState.getIntegerArrayList("PROGRESS_COLORS") ?: ArrayList()
        } else {
            val allQuestions = repository.loadQuestions().shuffled()
            questionsList = allQuestions.take(totalQuestionsToAsk)
        }

        answerButtons = listOf(
            binding.btnAnswer1,
            binding.btnAnswer2,
            binding.btnAnswer3,
            binding.btnAnswer4
        )

        setupProgressBar()
        displayQuestion()
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
            endQuiz()
            return
        }

        val currentQuestion = questionsList[currentQuestionIndex]
        binding.tvProgress.text = "Pytanie: ${currentQuestionIndex + 1} / ${questionsList.size}"
        binding.tvQuestion.text = "\"${currentQuestion.question}\""

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
                checkAnswer(button, answerText, currentQuestion.correctAnswer)
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
        } else {
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            progressColors.add(ContextCompat.getColor(this, R.color.red))
            // Highlight the correct answer
            answerButtons.forEach { button ->
                if (button.text == correctAnswer) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                }
            }
        }
        
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_INDEX", currentQuestionIndex)
        outState.putInt("SCORE", score)
        outState.putSerializable("QUESTIONS_LIST", ArrayList(questionsList))
        outState.putIntegerArrayList("PROGRESS_COLORS", progressColors)
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
}