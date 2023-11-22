package com.example.prueba.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.prueba.databinding.ActivityLandingLoginBinding
import com.parse.ParseAnonymousUtils
import com.parse.ParseException
import com.parse.ParseUser

class LandingLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLandingLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        login()
        setupLoginButton()
        //setupRegisterButton()
    }

    private fun login(){
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val sessionToken = sharedPreferences.getString("sessionToken", null)

        if (sessionToken != null) {
            ParseUser.becomeInBackground(sessionToken) { user: ParseUser?, _: ParseException? ->
                if (user != null) {
                    val toast = Toast.makeText(
                        this,
                        "Logged in as: ${user.username}",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()

                    user.put("state", "F")
                    user.saveInBackground { e ->
                        if (e == null) {
                            // do smth
                        } else {
                            // do smth
                        }
                    }

                    val intent = Intent (this, HomeActivity::class.java)
                    startActivity(intent)
                } else {
                    val toast = Toast.makeText(this,"Error logging in automatically",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    deleteSessionToken()
                }
            }
        } else {
            ParseAnonymousUtils.logIn { user, e ->
                if (e == null) {
                    val userId = user.objectId
                } else {
                    //
                }
                // anon user login
            }
        }
    }

    private fun deleteSessionToken() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("sessionToken")
        editor.apply()

        ParseAnonymousUtils.logIn { user, e ->
            if (e == null) {
                val userId = user.objectId
            } else {
                //
            }
        }
    }

    private fun validateForm(): Boolean {
        val u = binding.username.text.toString()
        val pw = binding.password.text.toString()

        return u.isNotEmpty() &&
                pw.isNotEmpty()
    }

    private fun setupLoginButton() {
        binding.login.setOnClickListener {
            if (validateForm()) {
                val u = binding.username.text.toString()
                val pw = binding.password.text.toString()

                ParseUser.logInInBackground(u, pw) { user: ParseUser?, e: ParseException? ->
                    if (user != null) {
                        val sessionToken = user.sessionToken
                        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("sessionToken", sessionToken)
                        editor.apply()

                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                    } else {
                        val toast = Toast.makeText(this, "Incorrect credentials!", Toast.LENGTH_SHORT)
                        toast.show()
                    }
                }
            } else {
                val toast = Toast.makeText(this, "Incorrect credentials!", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    /*
    private fun setupRegisterButton(){
        binding.register.setOnClickListener{
            val intent = Intent (this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
    */
}