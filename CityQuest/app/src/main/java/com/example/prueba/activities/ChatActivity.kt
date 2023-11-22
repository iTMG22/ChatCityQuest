package com.example.prueba.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.prueba.databinding.ActivityChatBinding
import com.parse.*
import com.parse.livequery.ParseLiveQueryClient
import com.parse.livequery.SubscriptionHandling

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageEditText: EditText
    private lateinit var chatContainer: LinearLayout
    private var idChatContrario: String = ""
    private lateinit var liveQueryClient: ParseLiveQueryClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombreReceptor = intent.getStringExtra("nombre").toString()
        val emailReceptor = intent.getStringExtra("email").toString()
        idChatContrario = intent.getStringExtra("imageUrl").toString()

        binding.recipientNameTextView.text = nombreReceptor
        if (idChatContrario.isNotEmpty()) {
            Glide.with(this).load(idChatContrario).into(binding.profileImageView)
        }

        messageEditText = binding.messageEditText
        chatContainer = binding.chatContainer

        setListeners()
        loadExistingMessages()
        initLiveQuery()
    }

    private fun setListeners() {
        binding.botonBack.setOnClickListener {
            onBackPressed() // Regresa a la actividad anterior
        }

        binding.sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val currentUser = ParseUser.getCurrentUser()
            val mensajeParse = ParseObject("Mensaje")
            mensajeParse.put("contenido", messageText)
            mensajeParse.put("id_emisor", currentUser.objectId)
            mensajeParse.put("id_receptor", idChatContrario)

            mensajeParse.saveInBackground { e ->
                if (e == null) {
                    runOnUiThread {
                        addMessageToChat(messageText, true)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Error al enviar el mensaje: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            messageEditText.text.clear()
        }
    }

    private fun loadExistingMessages() {
        val currentUser = ParseUser.getCurrentUser()
        val queryEmisorReceptor = ParseQuery.getQuery<ParseObject>("Mensaje")
        queryEmisorReceptor.whereEqualTo("id_emisor", currentUser.objectId)
        queryEmisorReceptor.whereEqualTo("id_receptor", idChatContrario)

        val queryReceptorEmisor = ParseQuery.getQuery<ParseObject>("Mensaje")
        queryReceptorEmisor.whereEqualTo("id_emisor", idChatContrario)
        queryReceptorEmisor.whereEqualTo("id_receptor", currentUser.objectId)

        val combinedQuery = ParseQuery.or(listOf(queryEmisorReceptor, queryReceptorEmisor))
        combinedQuery.orderByAscending("createdAt")

        combinedQuery.findInBackground { mensajes, e ->
            if (e == null) {
                runOnUiThread {
                    mensajes.forEach { mensaje ->
                        val contenidoMensaje = mensaje.getString("contenido") ?: ""
                        val emisor = mensaje.getString("id_emisor") ?: ""
                        addMessageToChat(contenidoMensaje, emisor == currentUser.objectId)
                    }

                    binding.scrollView.post {
                        binding.scrollView.fullScroll(View.FOCUS_DOWN)
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Error al cargar mensajes: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addMessageToChat(message: String, isSender: Boolean) {
        val messageTextView = TextView(this)
        messageTextView.text = message
        messageTextView.textAlignment = if (isSender) View.TEXT_ALIGNMENT_TEXT_END else View.TEXT_ALIGNMENT_TEXT_START
        chatContainer.addView(messageTextView)

        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun initLiveQuery() {
        try {
            liveQueryClient = ParseLiveQueryClient.Factory.getClient()
            Log.d("ChatActivity", "LiveQuery Client iniciado correctamente")
            subscribeToMessages()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error al iniciar LiveQuery: ${e.localizedMessage}")
        }
    }

    private fun subscribeToMessages() {
        val currentUser = ParseUser.getCurrentUser()
        val query = ParseQuery.getQuery<ParseObject>("Mensaje")
        query.whereEqualTo("id_receptor", currentUser.objectId)
        query.whereEqualTo("id_emisor", idChatContrario)

        val subscriptionHandling: SubscriptionHandling<ParseObject> = liveQueryClient.subscribe(query)

        subscriptionHandling.handleEvent(SubscriptionHandling.Event.CREATE) { _, mensaje ->
            runOnUiThread {
                Log.d("ChatActivity", "Nuevo mensaje recibido a través de LiveQuery")
                val contenidoMensaje = mensaje.getString("contenido") ?: ""
                val emisor = mensaje.getString("id_emisor") ?: ""

                addMessageToChat(contenidoMensaje, emisor == currentUser.objectId)
            }
        }
    }
}
