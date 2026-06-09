package com.example.quizgame

import android.content.Context
import org.json.JSONObject

class QuizRepository(private val context: Context) {

    private val authors: Map<String, Author>
    private val questions: List<Question>

    init {
        val (loadedAuthors, loadedQuestions) = loadData()
        authors = loadedAuthors
        questions = loadedQuestions
    }

    private fun loadData(): Pair<Map<String, Author>, List<Question>> {
        val jsonString = context.assets.open("questions.json")
            .bufferedReader()
            .use { it.readText() }

        val rootObject = JSONObject(jsonString)
        
        val authorsArray = rootObject.getJSONArray("authors")
        val authorsMap = mutableMapOf<String, Author>()
        for (i in 0 until authorsArray.length()) {
            val obj = authorsArray.getJSONObject(i)
            val author = Author(
                id = obj.getString("id"),
                name = obj.getString("name"),
                image = obj.getString("image")
            )
            authorsMap[author.id] = author
        }

        val questionsArray = rootObject.getJSONArray("questions")
        val questionsList = mutableListOf<Question>()
        for (i in 0 until questionsArray.length()) {
            val obj = questionsArray.getJSONObject(i)
            questionsList.add(
                Question(
                    question = obj.getString("question"),
                    authorId = obj.getString("authorId")
                )
            )
        }

        return Pair(authorsMap, questionsList)
    }

    fun getAllQuestions(): List<Question> = questions

    fun getAuthorName(authorId: String): String {
        return authors[authorId]?.name ?: "Nieznany autor"
    }

    fun getAuthorImage(authorId: String): String? {
        return authors[authorId]?.image
    }

    fun generateAnswersForQuestion(currentQuestion: Question): List<String> {
        val correctAuthorName = getAuthorName(currentQuestion.authorId)
        val answers = mutableListOf<String>()
        answers.add(correctAuthorName)

        val otherAuthorNames = authors.values
            .map { it.name }
            .filter { it != correctAuthorName }
            .shuffled()

        for (name in otherAuthorNames) {
            if (answers.size < 4) {
                answers.add(name)
            } else {
                break
            }
        }
        return answers.shuffled()
    }
}