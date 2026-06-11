package com.example.quizgame

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quizgame.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            showQuestionCountPopup()
        }

        binding.btnShowHighScores.setOnClickListener {
            showHighScoresTable()
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

    private fun showQuestionCountPopup() {
        val options = arrayOf("10 pytań", "20 pytań", "30 pytań")
        val values = intArrayOf(10, 20, 30)

        // Usunięcie setMessage() naprawia problem niewidocznych opcji w MaterialAlertDialog
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
    }
}