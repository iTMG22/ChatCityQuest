package com.example.prueba.util

import android.app.Application
import com.parse.Parse

class ParseConn : Application() {
    override fun onCreate() {
        super.onCreate()
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId("tHW5TLLoDiIlNQh75oW5kJ0y4joxdm5KhiYQ00aS")
                .clientKey("6O7O7t3lidiYXjG9n9CIh1fELdBV53xLnHqB0Rok")
                .server(IP)
                .build()
        )
    }

    companion object {
        const val IP = "https://parseapi.back4app.com/"
    }
}