package com.example.prueba.items
import java.io.Serializable

class User(var nombre: String? = null,
           var imageUrl: String? = null,
           var email: String? = null,
           var password: String? = null,
           var activo: Boolean = false,
           var apellido: String? = null,
           var latitude : Double = 0.0,
           var longitude : Double = 0.0
): Serializable
