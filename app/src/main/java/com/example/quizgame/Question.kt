package com.example.quizgame

import java.io.Serializable

data class Question(public val question: String, public val correctAnswer: String) : Serializable {
//    val question: String
//    val correctAnswer: String
//
//    constructor(question: String, correctAnswer: String) {
//        this.question = question
//        this.correctAnswer = correctAnswer
//    }
}