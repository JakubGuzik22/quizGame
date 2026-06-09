package com.example.quizgame

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.quizgame.databinding.ActivitySummaryBinding

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

        setupSummaryProgressBar(progressColors)

        binding.btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Clear the back stack so the user doesn't go back to the summary/quiz
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
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
}