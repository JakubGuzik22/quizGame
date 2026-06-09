package com.example.quizgame

import java.io.Serializable

data class Author(
    val id: String,
    val name: String,
    val image: String
) : Serializable