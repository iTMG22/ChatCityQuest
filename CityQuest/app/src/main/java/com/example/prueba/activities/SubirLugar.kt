package com.example.prueba.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.example.prueba.R
import com.example.prueba.databinding.ActivitySubirLugarBinding
import org.osmdroid.util.GeoPoint

class SubirLugar : AppCompatActivity() {

    private lateinit var binding: ActivitySubirLugarBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubirLugarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val latitude = intent.getDoubleExtra("latitude", 0.0) // 0.0 is a default value if the extra is not found
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        val imageView = findViewById<ImageView>(R.id.fotoSubida)

        val latitudText = findViewById<TextView>(R.id.latitud)
        val longitudeText = findViewById<TextView>(R.id.longitud)
        val formattedLatitud = String.format("%.2f", latitude)
        val formattedLongitud = String.format("%.2f", longitude)

        latitudText.text = "      Latitude: $formattedLatitud"
        longitudeText.text = "      Longitude: $formattedLongitud"

        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            val imageStream = contentResolver.openInputStream(imageUri)
            val decodedBitmap = BitmapFactory.decodeStream(imageStream)
            imageView.setImageBitmap(decodedBitmap)
        }

        val usuarioEditText = findViewById<EditText>(R.id.usuario)
        val userText = usuarioEditText.text.toString()

       binding.aceptar.setOnClickListener() {

           val intent = Intent(this, HomeActivity::class.java)
           intent.putExtra("nombre", userText)
           intent.putExtra("latitude", latitude)
           intent.putExtra("longitude", longitude)
           startActivity(intent)
       }

        binding.rechazar.setOnClickListener(){

            startActivity(Intent(baseContext, HomeActivity::class.java))
        }
    }


}