package com.example.quizgame

class Question {
    val question: String
    val correctAnswer: String

    constructor(question: String, correctAnswer: String) {
        this.question = question
        this.correctAnswer = correctAnswer
    }
}