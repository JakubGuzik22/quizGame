package com.example.quizgame

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class QuizRepository(private val context: Context) {

    private val questions: List<Question> = loadQuestions()
    private val allAnswers: List<String> = questions.map {it.correctAnswer}.distinct()

    fun loadQuestions(): List<Question> {
        val json = context.assets.open("questions.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(json)
        val questions = mutableListOf<Question>()

        for (i in 0 until jsonArray.length()) {
            val obj: JSONObject = jsonArray.getJSONObject(i)

            val question = obj.getString("question")
            val correctAnswer = obj.getString("correctAnswer")

            questions.add(Question(question, correctAnswer))
        }
        return questions
    }

    fun generateAnswersForQuestion(currentQuestion: Question): List<String> {
        val answers = mutableListOf<String>()
        answers.add(currentQuestion.correctAnswer)

        val wrongAnswersPool = allAnswers.filter { it != currentQuestion.correctAnswer }.shuffled()

        for (answer in wrongAnswersPool) {
            if (answers.size < 4){
                answers.add(answer)
            } else {
                break
            }
        }
        return answers.toList().shuffled()
    }

}