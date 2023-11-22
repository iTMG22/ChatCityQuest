package com.example.prueba.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.prueba.databinding.ActivityHomeBinding
import com.example.prueba.R
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private var cameraUri: Uri? = null
class HomeActivity : AppCompatActivity(), SensorEventListener {
    private val MIN_DISTANCE_FOR_UPDATE = 15.0
    private val JSON_FILE_NAME = "location_records.json"
    // private val TAG = "MapActivity"
    private var lastLocation: Location? = null
    private lateinit var binding: ActivityHomeBinding
    private lateinit var map: MapView
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private var currentMarker: Marker? = null
    private lateinit var locationManager: LocationManager
    private val REQUEST_LOCATION_PERMISSION = 1
    private val geocoder: Geocoder by lazy { Geocoder(this) }
    private lateinit var addressEditText: EditText
    private lateinit var searchButton: ImageButton
    private val userLocationMarkers = ArrayList<Marker>()
    private val searchMarkers = ArrayList<Marker>()
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null
    private lateinit var linearAceleration: Sensor
    private lateinit var orientationSensor: Sensor
    // private val bogota = GeoPoint(4.62, -74.07)
    private lateinit var userGeoPoint: GeoPoint
    private lateinit var direccion: String
    private var marker: Marker? = null
    private val predefinedMarkers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.overlays.add(createOverlayEvents())
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        linearAceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!
        addressEditText = findViewById(R.id.addressEditText)
        searchButton = findViewById(R.id.searchButton)
        searchButton.setOnClickListener {
            val address = addressEditText.text.toString()
            searchLocation(address)

            val foundLocation = searchMarkers.any { it.title == address }

            if (!foundLocation) {
                searchPredefinedMarkerByName(address)
            }
        }

        roadManager = OSRMRoadManager(this, "ANDROID")
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        userGeoPoint = GeoPoint(4.62, -74.07)

        direccion = "North"

        binding.subirFoto.setOnClickListener{

            val file = File(filesDir, "picFromCamera")
            cameraUri = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
            getContentCamera.launch(cameraUri)
        }

        val intent = intent
        if (intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)
            val string = intent.getStringExtra("nombre")

            addPointOfInterest(latitude,longitude,string!!)
        }


        binding.perfil.setOnClickListener(){

            startActivity(Intent(baseContext,PerfilActivity::class.java))
        }

        binding.chatlist.setOnClickListener(){

            startActivity(Intent(baseContext,ChatListActivity::class.java))
        }

        binding.tiendaPuntos.setOnClickListener(){

            startActivity(Intent(baseContext,PuntosActivity::class.java))
        }

        binding.homeButton.setOnClickListener(){

            startActivity(Intent(baseContext, LoginActivity::class.java))
        }

        binding.notificaciones.setOnClickListener(){

            startActivity(Intent(baseContext, NotificacionesActivity::class.java))
        }
    }

    private val getContentCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Save the image to internal storage
            val savedImageUri = saveImageToInternalStorage(cameraUri!!)
            // Pass the URI of the saved image to the next activity
            startNextActivity(savedImageUri)
        }
    }

    private fun startNextActivity(savedImageUri: Uri) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                // val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                val intent = Intent(this, SubirLugar::class.java)
                intent.putExtra("latitude", location.latitude)
                intent.putExtra("longitude", location.longitude)
                intent.putExtra("imageUri", savedImageUri.toString())
                startActivity(intent)

            }

        }
    }

    private fun saveImageToInternalStorage(cameraUri: Uri): Uri {
        val inputStream = contentResolver.openInputStream(cameraUri)
        val file = File(filesDir, "captured_image.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, linearAceleration, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,   // Tiempo mínimo entre actualizaciones en milisegundos (1 segundo en total)
                10.0f,  // Distancia mínima entre actualizaciones en metros
                locationListener
            )
            map.onResume()
            map.controller.setZoom(18.0)
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.animateTo(userGeoPoint)
            }
            showUserLocation()
            addPredefinedMarkers()

            if (searchMarkers.isNotEmpty()) {
                val destination = searchMarkers.firstOrNull()?.position
                if (destination != null) {
                    val userLocation = userLocationMarkers.firstOrNull()?.position
                    if (userLocation != null) {
                        drawRoute(userLocation, destination)
                    }
                }
            }
        } else {
            map.onResume()
            map.controller.setZoom(18.0)
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(this)
                .setTitle("Permiso de ubicación necesario")
                .setMessage("La aplicación necesita acceder a su ubicación para mostrar el mapa.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                .setNegativeButton("Cancelar") { _, _ ->
                }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onResume()
                } else {
                    // El usuario denegó el permiso
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_LIGHT){
            val lightValue = event.values[0]
            val threshold = 80.0
            if(lightValue < threshold){
                map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }else{
                map.overlayManager.tilesOverlay.setColorFilter(null)
            }
        }

        if(event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            val azimuthRadians = orientationValues[0]
            val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
            val adjustedAzimuth = if (azimuthDegrees < 0) azimuthDegrees + 360 else azimuthDegrees
            direccion = mapHeadingToDirection(adjustedAzimuth)
            val directionTextView2 = findViewById<TextView>(R.id.orientacion)
            directionTextView2.text = direccion
            val myIcon: Drawable? = when (direccion) {
                "East" -> ContextCompat.getDrawable(this, R.drawable.right)
                "West" -> ContextCompat.getDrawable(this, R.drawable.left)
                "North" -> ContextCompat.getDrawable(this, R.drawable.up)
                "South" -> ContextCompat.getDrawable(this, R.drawable.down)
                else -> ContextCompat.getDrawable(this, R.drawable.outward)
            }

            marker?.icon = myIcon


        }

        if(event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION){

            val accelerationX = event.values[0]
            val accelerationY = event.values[1]
            val accelerationZ = event.values[2]
            val accelerationMagnitude = sqrt(
                (accelerationX * accelerationX +
                        accelerationY * accelerationY +
                        accelerationZ * accelerationZ).toDouble()
            )

            val formattedAccelerationMagnitude = String.format("%.2f", accelerationMagnitude)

            val directionTextView = findViewById<TextView>(R.id.aceleracion)
            directionTextView.text = accelerationMagnitude.toString()

            directionTextView.text = formattedAccelerationMagnitude

            if(accelerationMagnitude > 14){

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (userLocation != null) {
                        /*
                        val proximityRadius = 5.0

                        val userLatitude = userLocation.latitude
                        val userLongitude = userLocation.longitude

                        // Arreglar Toast por proximidad
                        if(searchPredefinedMarkerByCoordinates(userLatitude, userLongitude)){

                            Toast.makeText(this, "Felicidades, has encontrado una bella obra de arte", Toast.LENGTH_SHORT).show()
                        }else{

                            Toast.makeText(this,"Sigue buscando", Toast.LENGTH_SHORT).show()
                        }
                        */
                    }
                }
            }
        }
    }

    private fun updateRoute(start: GeoPoint, finish: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("MapsApp", "Route length: " + road.mLength + " klm")
        Log.i("MapsApp", "Duration: " + road.mDuration / 60 + " min")
        if (roadOverlay != null) {
            map.overlays.remove(roadOverlay)
        }
        roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay!!.outlinePaint.color = Color.CYAN
        roadOverlay!!.outlinePaint.strokeWidth = 10F
        map.overlays.add(roadOverlay)
        map.invalidate()
    }

    private val locationListener: LocationListener = LocationListener { location ->
        val latitude = location.latitude
        val longitude = location.longitude
        val userGeoPoint = GeoPoint(latitude, longitude)
        userLocationMarkers.forEach { map.overlays.remove(it) }
        userLocationMarkers.clear()
        val address = "Ubicación Actual"
        marker = createMarker(userGeoPoint, address, R.drawable.arrowofuser,direccion)
        userLocationMarkers.add(marker!!)
        map.overlays.add(marker!!)

        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            if (distance > MIN_DISTANCE_FOR_UPDATE) {
                saveLocationRecord(location)
            }
        }
        lastLocation = location

        if (searchMarkers.isNotEmpty()) {
            val searchMarker = searchMarkers.first()
            val searchGeoPoint = searchMarker.position
            updateRoute(userGeoPoint, searchGeoPoint)
        }
    }

    private fun createOverlayEvents(): MapEventsOverlay {
        return MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    longPressOnMap(p)
                }
                return true
            }
        })
    }

    private fun longPressOnMap(p: GeoPoint) {
        currentMarker?.title = ""
        val addressText = findAddress(p)
        val titleText: String = addressText ?: ""

        if (currentMarker == null) {
            currentMarker = createMarker(p, titleText, R.drawable.puntero2,direccion)
            searchMarkers.add(currentMarker!!)
            map.overlays.add(currentMarker)
        } else {
            currentMarker?.title = titleText
            currentMarker?.position = p
        }
        val userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (userLocation != null) {
            val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
            val distance = calculateDistance(userGeoPoint, p)
            val distanceMessage = "Distancia total entre puntos: $distance km"
            Toast.makeText(this, distanceMessage, Toast.LENGTH_SHORT).show()
        }
        val address = findAddress(p)
        val snippet: String = address ?: ""
        searchMarkers.forEach { map.overlays.remove(it) }
        searchMarkers.clear()

        val marker = createMarker(p, snippet, R.drawable.arrowofuser,direccion)
        searchMarkers.add(marker)
        map.overlays.add(marker)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                updateRoute(userGeoPoint, p)
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun findAddress(latLng: LatLng): String? {
        val addresses: List<Address> = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) ?: emptyList()
        if (addresses.isNotEmpty()) {
            val address: Address = addresses[0]
            return address.getAddressLine(0)
        }
        return null
    }

    private fun calculateDistance(start: GeoPoint, finish: GeoPoint): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en kilómetros
        val dLat = Math.toRadians(finish.latitude - start.latitude)
        val dLng = Math.toRadians(finish.longitude - start.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(finish.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun createMarker(p: GeoPoint, title: String, iconID: Int): Marker {
        val marker = Marker(map)
        marker.title = title
        val myIcon = ContextCompat.getDrawable(this, iconID)
        marker.icon = myIcon
        marker.position = p
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        return marker
    }

    private fun findAddress(geoPoint: GeoPoint): String? {
        val addresses: List<Address> = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1) ?: emptyList()
        if (addresses.isNotEmpty()) {
            val address: Address = addresses[0]
            return address.getAddressLine(0)
        }
        return null
    }

    private fun searchLocation(address: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (userLocation != null) {
                val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                val geocodeResults = geocoder.getFromLocationName(address, 1)
                if (!geocodeResults.isNullOrEmpty() && geocodeResults[0] != null) {
                    val foundAddress = geocodeResults[0]!!
                    val latitude = foundAddress.latitude
                    val longitude = foundAddress.longitude
                    val geoPoint = GeoPoint(latitude, longitude)
                    val addressAsTitle = foundAddress.getAddressLine(0)

                    drawRoute(userGeoPoint, geoPoint)
                    searchMarkers.forEach { map.overlays.remove(it) }
                    searchMarkers.clear()

                    val marker = createMarker(geoPoint, addressAsTitle, R.drawable.puntero2,direccion)

                    searchMarkers.add(marker)
                    map.overlays.add(marker)

                    val distance = calculateDistance(userGeoPoint, geoPoint)
                    val distanceMessage = "Distancia total entre puntos: $distance km"

                    Toast.makeText(this, distanceMessage, Toast.LENGTH_SHORT).show()
                    map.controller.animateTo(geoPoint)
                } else {
                    Toast.makeText(this, "Buscando localizacion puesta por los usarios", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                marker = createMarker(userGeoPoint, "Mi Ubicación", R.drawable.arrowofuser,direccion)
                userLocationMarkers.add(marker!!)
                map.overlays.add(marker!!)
            }
        }
    }

    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("MapsApp", "Route length: " + road.mLength + " klm")
        Log.i("MapsApp", "Duration: " + road.mDuration / 60 + " min")
        if (roadOverlay != null) {
            map.overlays.remove(roadOverlay) // Elimino a la ruta anterior
        }
        roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay!!.outlinePaint.color = Color.CYAN
        roadOverlay!!.outlinePaint.strokeWidth = 10F
        map.overlays.add(roadOverlay) // Agrego la nueva ruta
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveLocationRecord(location: Location) {
        try {
            val locationRecord = JSONObject()
            locationRecord.put("latitude", location.latitude)
            locationRecord.put("longitude", location.longitude)
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            locationRecord.put("timestamp", currentTime)
            var jsonArray = JSONArray()
            val file = File(filesDir, JSON_FILE_NAME)
            if (file.exists()) {
                val jsonStr = FileReader(file).readText()
                jsonArray = JSONArray(jsonStr)
            }
            jsonArray.put(locationRecord)
            FileWriter(file).use { it.write(jsonArray.toString()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showJsonContents() {
        val file = File(filesDir, JSON_FILE_NAME)
        if (file.exists()) {
            try {
                val jsonArray: JSONArray   // Acà declaro un jsonArray como una variable mutable.
                val jsonStr = file.readText()
                jsonArray = JSONArray(jsonStr)  // Acà asigno  el contenido del archivo .json a un jsonArray.
                for (i in 0 until jsonArray.length()) {
                    val locationRecord = jsonArray.getJSONObject(i)
                    val latitude = locationRecord.getDouble("latitude")
                    val longitude = locationRecord.getDouble("longitude")
                    val timestamp = locationRecord.getString("timestamp")
                    // Acà miestro los datos como tal de cada una de las lineas del archivo.
                    val message = "Latitud: $latitude\nLongitud: $longitude\nTimestamp: $timestamp"
                    showAlertDialog("Registro de Ubicación", message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Por si es que el archivo JSON no existe, muestro un mensaje.
            showAlertDialog("Archivo no encontrado", "No se encontró el archivo JSON.")
        }
    }

    // Acà muestro la ruta en el mapa basada en los registros de ubicación guardados en el archivo .json.
    /*private fun showLocationRoute() {
        if (jsonFile?.exists() == true) {
            // Leo el contenido del archivo .json.
            val jsonStr = FileReader(jsonFile).readText()
            try {
                val jsonArray = JSONArray(jsonStr)
                val routePoints = ArrayList<GeoPoint>()
                for (i in 0 until jsonArray.length()) {
                    val locationRecord = jsonArray.getJSONObject(i)
                    val latitude = locationRecord.getDouble("latitude")
                    val longitude = locationRecord.getDouble("longitude")
                    // Agrego la ubicación a la lista de puntos de la ruta.
                    routePoints.add(GeoPoint(latitude, longitude))
                }
                if (routePoints.size >= 2) {
                    // Acà por lo que leì estoy creando una "polilínea" que conecte a todos los puntos de la ruta del archivo .json.
                    val routePolyline = Polyline()
                    routePolyline.setPoints(routePoints)
                    routePolyline.color = Color.YELLOW  // Cambiar el color a amarillo
                    routePolyline.width = 5.0f
                    // Acà agrego la polilínea al mapa
                    map.overlays.add(routePolyline)
                    map.invalidate()
                    // Ajusto el zoom para mostrar toda la ruta
                    map.zoomToBoundingBox(routePolyline.bounds, true)
                    // Para hacer aun màs funcional odo acà programè la eliminación de la ruta después de 5 segundos de su apariciòn-
                    Handler().postDelayed({
                        if (map.overlays.contains(routePolyline)) {
                            map.overlays.remove(routePolyline)
                            map.invalidate()
                            // Toast.makeText(this, "La ruta se ha eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }, 5000) // 5000 ms = 5 segundos
                } else {
                    Toast.makeText(this, "No hay suficientes registros de ubicación para mostrar una ruta.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Y si es que el archivo .json no existe, muestra un mensaje apropiado
            showAlertDialog("Archivo no encontrado", "No se encontró el archivo JSON.")
        }
    }
*/
    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createMarker(p: GeoPoint, title: String, iconID: Int, direction: String): Marker {
        val marker = Marker(map)
        marker.title = title

        marker.position = p

        val myIcon: Drawable? = when (direction) {
            "North" -> ContextCompat.getDrawable(this, R.drawable.up)
            "South" -> ContextCompat.getDrawable(this, R.drawable.down)
            "West" -> ContextCompat.getDrawable(this, R.drawable.right)
            "East" -> ContextCompat.getDrawable(this, R.drawable.left)
            else -> ContextCompat.getDrawable(this, R.drawable.outward)
        }

        marker.icon = myIcon

        return marker

    }

    private fun mapHeadingToDirection(heading: Float): String {
        return when (heading) {
            in 337.5..22.5, in 0.0..22.5 -> "North"
            in 22.5..67.5 -> "Northeast"
            in 67.5..112.5 -> "East"
            in 112.5..157.5 -> "Southeast"
            in 157.5..202.5 -> "South"
            in 202.5..247.5 -> "Southwest"
            in 247.5..292.5 -> "West"
            in 292.5..337.5 -> "Northwest"
            else -> "Unknown"
        }
    }

    private fun addPredefinedMarkers() {
        addPointOfInterest(4.5981, -74.0755, "Museo del Oro")
        addPointOfInterest(4.6037, -74.0666, "Monserrate")
        addPointOfInterest(4.5921, -74.0746, "Plaza de Bolívar")
        addPointOfInterest(4.6477, -74.0839, "Jardín Botánico")
        addPointOfInterest(4.6610, -74.0937, "Parque Simón Bolívar")
        addPointOfInterest(4.6512, -74.0939, "Nuestra Ruta")

        // Registra clics en los marcadores
        for (marker in predefinedMarkers) {
            marker.setOnMarkerClickListener { _, _ ->
                // Obtén la ubicación del marcador seleccionado
                val destination = GeoPoint(marker.position.latitude, marker.position.longitude)
                // Obtén la ubicación actual
                val userLocation = userLocationMarkers.firstOrNull()?.position
                if (userLocation != null) {
                    // Dibuja la ruta desde la ubicación actual al destino
                    drawRoute(userLocation, destination)
                }
                true // Devuelve true para indicar que has manejado el click
            }
        }
    }

    private fun addPointOfInterest(latitude: Double, longitude: Double, title: String) {
        val poiGeoPoint = GeoPoint(latitude, longitude)
        val marker = createMarker(poiGeoPoint, title, R.drawable.punto_ruta)
        predefinedMarkers.add(marker)
        map.overlays.add(marker)
    }

    private fun searchPredefinedMarkerByName(name: String) {
        for (marker in predefinedMarkers) {
            if (marker.title == name) {
                // Toma la locacion por el nombre definido por el usuario
                val location = GeoPoint(marker.position.latitude, marker.position.longitude)

                // Dibuja la ruta
                val userLocation = userLocationMarkers.firstOrNull()?.position
                if (userLocation != null) {
                    drawRoute(userLocation, location)
                }

                // Se va a la ruta donde esta el usuario
                map.controller.animateTo(location)

                // Exit the loop once a matching marker is found.
                break
            }
        }
    }
}
