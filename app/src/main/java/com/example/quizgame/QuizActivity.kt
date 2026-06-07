package com.example.quizgame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuizActivity : AppCompatActivity() {
    private lateinit var repository: QuizRepository
    private lateinit var questionsList: List<Question>

    private var currentQuestionIndex = 0
    private var score = 0
    
    private lateinit var tvQuestion: TextView
    private lateinit var tvProgress: TextView
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    private lateinit var answerButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        repository = QuizRepository(this)
        questionsList = repository.loadQuestions().shuffled()

        tvQuestion = findViewById(R.id.tvQuestion)
        tvProgress = findViewById(R.id.tvProgress)
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)

        answerButtons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)

        displayQuestion()
    }

    private fun displayQuestion() {
        if (currentQuestionIndex >= questionsList.size) {
            endQuiz()
            return
        }

        val currentQuestion = questionsList[currentQuestionIndex]
        tvProgress.text = "Pytanie: ${currentQuestionIndex + 1} / ${questionsList.size}"
        tvQuestion.text = currentQuestion.question

        val generatedAnswers = repository.generateAnswersForQuestion(currentQuestion)

        for (i in answerButtons.indices) {
            val answerText = generatedAnswers[i]
            answerButtons[i].text = answerText

            answerButtons[i].setOnClickListener {
                checkAnswer(answerText, currentQuestion.correctAnswer)
            }
        }
    }

    private fun checkAnswer(selectedAnswer: String, correctAnswer: String) {
        if (selectedAnswer == correctAnswer) {
            score++
            Toast.makeText(this, "Poprawna odpowiedź!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Błąd! Prawidłowo: $correctAnswer", Toast.LENGTH_SHORT).show()
        }

        currentQuestionIndex++
        displayQuestion()
    }

    private fun endQuiz() {
        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL_QUESTIONS", questionsList.size)
        }
        startActivity(intent)
        finish()
    }
}