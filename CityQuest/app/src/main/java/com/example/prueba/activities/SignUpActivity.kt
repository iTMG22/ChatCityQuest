package com.example.prueba.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.prueba.databinding.ActivitySignUpBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.parse.ParseACL
import com.parse.ParseException
import com.parse.ParseUser
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding

    var name = ""
    var email = ""
    private var surname = ""
    var latitude = 0.0
    var longitude = 0.0

    lateinit var lastLocation : Location
    private val locationPermissionCode = 1

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var imageSelected = false

    private lateinit var uriUpload : Uri
    private lateinit var cameraUri : Uri

    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            loadImage(it!!)
            cameraUri = it
            imageSelected = true
        }

    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                loadImage(cameraUri)
                imageSelected = true
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gallery.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.camera.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallBack()

        startLocationUpdates()
        setupRegisterButton()

        binding.regImageView.setOnClickListener{
            getContentGallery.launch("image/*")
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.regName.text.toString()
        val surname = binding.regSurname.text.toString()
        val email = binding.regEmail.text.toString()
        val password = binding.regPw.text.toString()
        val confirmPassword = binding.regConfirmPw.text.toString()

        val fieldsHaveText = name.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty()
                && password.isNotEmpty() && confirmPassword.isNotEmpty()
        val validEmail = isValidEmail(email)
        val match = password == confirmPassword

        return fieldsHaveText && match && validEmail && imageSelected
    }

    private fun setupRegisterButton() {
        binding.regButton.setOnClickListener {
            if (validateForm()) {
                saveUser()
                /*
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
                */
            } else {
                val toast = Toast.makeText(this, "Every field must have text. Don't forget to add a profile picture", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    private fun saveUser() {
        val u = ParseUser()
        u.username = binding.regEmail.text.toString()
        u.setPassword(binding.regPw.text.toString())

        name = binding.regName.text.toString()
        u.put("name", name)

        surname = binding.regSurname.text.toString()
        u.put("surname", surname)

        u.put("state", "F")

        u.put("latitude", latitude)
        u.put("longitude", longitude)

        val acl = ParseACL()
        acl.publicReadAccess = true
        u.acl = acl

        u.signUpInBackground { e: ParseException? ->
            if (e == null) {
                val sessionToken = u.sessionToken
                val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("sessionToken", sessionToken)
                editor.apply()

                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("email", u.username.toString())
                startActivity(intent)
                finish()

                uploadFirebaseImage(uriUpload)
            } else {
                // log smth
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$".toRegex()
        return email.matches(emailRegex)
    }

    private fun uploadFirebaseImage(uriUpload: Uri) {
        val currentUser = ParseUser.getCurrentUser()
        val objectId = currentUser?.objectId
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child("images/${objectId}.png")

        storageRef.putFile(uriUpload)
            .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl
                downloadUrl?.addOnSuccessListener { uri ->
                    println("Image uploaded. URL: $uri")
                }
            }
            .addOnFailureListener { exception: Exception ->
                println("Error uploading: ${exception.message}")
            }
    }

    private fun createLocationRequest() : LocationRequest{
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(1000)
            .build()
        return locationRequest
    }

    private fun createLocationCallBack() : LocationCallback{
        val locationCallback = object : LocationCallback(){
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                lastLocation = result.lastLocation!!
                latitude = lastLocation.latitude
                longitude = lastLocation.longitude
            }
        }
        return locationCallback
    }

    private fun startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                val photoFile = createImageFile()
                if (photoFile != null) {
                    cameraUri = FileProvider.getUriForFile(
                        this,
                        "com.example.prueba.fileprovider",
                        photoFile
                    )

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                    getContentCamera.launch(cameraUri)
                } else {
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null) {
                binding.regImageView.setImageBitmap(rotateImageIfRequired(bitmap, uri))
                uriUpload = uri
            } else {
                Toast.makeText(this, "Error decoding image", Toast.LENGTH_SHORT).show()
            }

            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rotateImageIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        val ei = ExifInterface(contentResolver.openInputStream(uri)!!)

        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}