package com.example.quizgame

import android.content.Intent
import android.os.Bundle
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