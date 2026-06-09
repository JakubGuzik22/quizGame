package com.example.quizgame

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.quizgame.databinding.ActivitySummaryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL_QUESTIONS", 0)
        val progressColors = intent.getIntegerArrayListExtra("PROGRESS_COLORS") ?: arrayListOf<Int>()

        binding.tvScore.text = "Twój wynik: $score / $total"
        
        handleBestScore(score, total)

        setupSummaryProgressBar(progressColors)

        binding.btnPlayAgain.setOnClickListener {
            showQuestionCountPopup()
        }

        binding.btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Clear the back stack so the user doesn't go back to the summary/quiz
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun handleBestScore(currentScore: Int, currentTotal: Int) {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val bestScore = sharedPref.getInt("BEST_SCORE", 0)
        val bestTotal = sharedPref.getInt("BEST_TOTAL", 0)

        // Porównujemy procentowo, żeby rekord był sprawiedliwy niezależnie od liczby pytań
        val currentPercent = if (currentTotal > 0) currentScore.toFloat() / currentTotal else 0f
        val bestPercent = if (bestTotal > 0) bestScore.toFloat() / bestTotal else 0f

        if (currentPercent >= bestPercent) {
            with(sharedPref.edit()) {
                putInt("BEST_SCORE", currentScore)
                putInt("BEST_TOTAL", currentTotal)
                apply()
            }
            binding.tvBestScore.text = "Nowy rekord: $currentScore / $currentTotal"
        } else {
            binding.tvBestScore.text = "Najlepszy wynik: $bestScore / $bestTotal"
        }
    }

    private fun setupSummaryProgressBar(colors: ArrayList<Int>) {
        binding.llSummaryProgressBar.removeAllViews()
        for (i in colors.indices) {
            val segment = View(this)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            if (i < colors.size - 1) {
                params.marginEnd = 4
            }
            segment.layoutParams = params
            segment.setBackgroundColor(colors[i])
            binding.llSummaryProgressBar.addView(segment)
        }
    }

    private fun showQuestionCountPopup() {
        val options = arrayOf("10 pytań", "20 pytań", "30 pytań")
        val values = intArrayOf(10, 20, 30)

        MaterialAlertDialogBuilder(this)
            .setTitle("Ile pytań chcesz dzisiaj pokonać?")
            .setItems(options) { _, which ->
                val selectedCount = values[which]
                startQuiz(selectedCount)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun startQuiz(count: Int) {
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("QUESTION_COUNT", count)
        }
        startActivity(intent)
        finish()
    }
}