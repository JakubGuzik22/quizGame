package com.example.quizgame

import java.io.Serializable

data class Question(
    val question: String,
    val authorId: String
) : Serializable