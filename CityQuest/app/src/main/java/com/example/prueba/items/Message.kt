package com.example.prueba.items
import java.util.Date
class Message(
    var text: String,
    var senderId: String,
    var recipientId: String,
    var createdAt: Date = Date()
)
