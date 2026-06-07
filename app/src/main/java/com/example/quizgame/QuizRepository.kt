package com.example.quizgame

import android.content.Context
import org.json.JSONArray

class QuizRepository(private val context: Context) {

    fun loadQuestions(): List<Pair<Question, List<String>>> {
        val json = context.assets.open("questions.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(json)
        val questions = mutableListOf<Question>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            questions.add(
                Question(
                    question = obj.getString("question"),
                    correctAnswer = obj.getString("correctAnswer")
                )
            )
        }

        val shuffledQuestions = questions.shuffled()
        val allAnswers = shuffledQuestions.map { it.correctAnswer }

        // Dla każdego pytania losujemy 3 błędne odpowiedzi z pozostałych
        return shuffledQuestions.map { current ->
            val wrongAnswers = allAnswers
                .filter { it != current.correctAnswer }
                .shuffled()
                .take(3)

            // Mieszamy poprawną z błędnymi, żeby nie była zawsze na tej samej pozycji
            val options = (wrongAnswers + current.correctAnswer).shuffled()

            Pair(current, options)
        }
    }
}