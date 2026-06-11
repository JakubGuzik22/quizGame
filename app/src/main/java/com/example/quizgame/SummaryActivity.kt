package com.example.quizgame

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.quizgame.databinding.ActivitySummaryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("DARK_MODE", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL_QUESTIONS", 0)
        val progressColors = intent.getIntegerArrayListExtra("PROGRESS_COLORS") ?: arrayListOf<Int>()
        val opponentScore = intent.getIntExtra("OPPONENT_SCORE", -1)
        val isMultiplayer = intent.getBooleanExtra("IS_MULTIPLAYER", false)

        binding.tvScore.text = "Twój wynik: $score / $total"
        
        if (score == total && total > 0) {
            playPerfectScoreSound()
        }
        
        if (isMultiplayer) {
            handleMultiplayerResult(score, opponentScore, total)
        } else {
            handleBestScore(score, total)
        }

        setupSummaryProgressBar(progressColors)

        binding.btnPlayAgain.setOnClickListener {
            showQuestionCountPopup()
        }

        binding.btnShowHighScores.setOnClickListener {
            showHighScoresTable()
        }

        binding.btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Clear the back stack so the user doesn't go back to the summary/quiz
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun handleMultiplayerResult(currentScore: Int, opponentScore: Int, total: Int) {
        val resultText = when {
            currentScore > opponentScore -> "WYGRAŁEŚ! 🏆 (Przeciwnik: $opponentScore)"
            currentScore < opponentScore -> "PRZEGRAŁEŚ... 💀 (Przeciwnik: $opponentScore)"
            else -> "REMIS! 🤝 (Przeciwnik: $opponentScore)"
        }
        binding.tvBestScore.text = resultText
        binding.tvBestScore.setTextColor(if (currentScore >= opponentScore) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
    }

    private fun handleBestScore(currentScore: Int, currentTotal: Int) {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val keyBestScore = "BEST_SCORE_$currentTotal"
        
        // Domyślnie -1, żeby wiedzieć czy to pierwszy raz w tym trybie
        val bestScoreForTotal = sharedPref.getInt(keyBestScore, -1)

        if (currentScore >= bestScoreForTotal) {
            val isNewRecord = currentScore > bestScoreForTotal && bestScoreForTotal != -1
            
            with(sharedPref.edit()) {
                putInt(keyBestScore, currentScore)
                apply()
            }

            binding.tvBestScore.text = when {
                bestScoreForTotal == -1 -> "Pierwszy wynik w trybie $currentTotal pytań!"
                isNewRecord -> "Nowy rekord trybu $currentTotal pytań!"
                else -> "Wyrównany rekord trybu $currentTotal pytań!"
            }
        } else {
            binding.tvBestScore.text = "Najlepszy wynik: $bestScoreForTotal / $currentTotal"
        }
    }

    private fun showHighScoresTable() {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val categories = intArrayOf(10, 20, 30)
        
        val tableLayout = TableLayout(this).apply {
            setPadding(40, 20, 40, 20)
        }

        // Nagłówek tabeli
        val headerRow = TableRow(this).apply {
            addView(createTableCell("Tryb", true))
            addView(createTableCell("Wynik", true))
            addView(createTableCell("%", true))
        }
        tableLayout.addView(headerRow)

        // Dane dla każdej kategorii
        for (count in categories) {
            val score = sharedPref.getInt("BEST_SCORE_$count", -1)
            val row = TableRow(this).apply {
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, 10, 0, 10)
                
                addView(createTableCell("$count pytań", false))
                
                if (score != -1) {
                    val percent = (score.toFloat() / count * 100).toInt()
                    addView(createTableCell("$score / $count", false))
                    addView(createTableCell("$percent%", false))
                } else {
                    addView(createTableCell("-", false))
                    addView(createTableCell("-", false))
                }
            }
            tableLayout.addView(row)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Twoje Najlepsze Wyniki")
            .setView(tableLayout)
            .setPositiveButton("Zamknij", null)
            .show()
    }

    private fun createTableCell(text: String, isHeader: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(20, 10, 20, 10)
            gravity = Gravity.CENTER
            if (isHeader) {
                setTypeface(null, Typeface.BOLD)
                textSize = 16f
            } else {
                textSize = 14f
            }
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

    private fun playPerfectScoreSound() {
        val sharedPref = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val isSoundEnabled = sharedPref.getBoolean("SOUND_ENABLED", true)
        if (!isSoundEnabled) return

        mediaPlayer = MediaPlayer.create(this, R.raw.perfect_score)
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}