package com.example.geofencemonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geofencemonitor.Model.GeofenceModel
import com.example.geofencemonitor.ViewModel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.HashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val isServiceActivated = false

    private var gMap: GoogleMap? = null
    var LAT: Double = 0.0
    var LNG: Double = 0.0
    var GEOFENCE_RADIUS_IN_METERS: Int = 0
    val AREA_LANDMARKS = HashMap<String, LatLng>()
    val GEOFENCE_ID_ENA_VILLAGE = "ENA_VILLAGE"
    lateinit var LATLNG_ENA_VILLAGE: LatLng

    private val viewModel: MainViewModel by viewModels()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                BackgroundService.ACTION_CONNECTED -> addCircle()
                BackgroundService.ACTION_LOCATION -> {
                    addCircle()
                    updateLocation(
                        intent.getDoubleExtra("CURRENT_LAT", 0.0),
                        intent.getDoubleExtra("CURRENT_LNG", 0.0)
                    )
                }
            }
        }
    }

    private fun addCircle() {

        Log.d("ADD_CIRCLE", "addCircle: CALL")
        gMap!!.addCircle(
            CircleOptions()
                .center(LatLng(LATLNG_ENA_VILLAGE.latitude, LATLNG_ENA_VILLAGE.longitude))
                .radius(GEOFENCE_RADIUS_IN_METERS.toDouble())
                .strokeColor(Color.RED)
                .strokeWidth(5f)
        )
    }

    private fun updateLocation(LAT: Double, LNG: Double) {
        gMap!!.addMarker(
            MarkerOptions()
                .position(LatLng(LAT, LNG))
                .title("Ena Village")
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Service Started Successfully!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@MainActivity, BackgroundService::class.java)
            intent.putExtra("GEOFENCE_LAT", LAT)
            intent.putExtra("GEOFENCE_LNG", LNG)
            intent.putExtra("GEOFENCE_RADIUS", GEOFENCE_RADIUS_IN_METERS)
            ContextCompat.startForegroundService(this@MainActivity, intent)
        } else {
            Toast.makeText(
                this,
                "Do I really need to tell, why you should give me location access??",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("ADD_CIRCLE", "onCreate: ")

        val bundle: Bundle? = intent.extras
        LAT = bundle?.get("GEOFENCE_LAT") as Double
        LNG = bundle?.get("GEOFENCE_LNG") as Double
        GEOFENCE_RADIUS_IN_METERS = bundle.get("GEOFENCE_RADIUS") as Int

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        if (!isServiceActivated) {
//            Toast.makeText(
//                this@MainActivity,
//                "Service Started Successfully!",
//                Toast.LENGTH_SHORT
//            ).show()

            val intent = Intent(this@MainActivity, BackgroundService::class.java)
            intent.putExtra("GEOFENCE_LAT", LAT)
            intent.putExtra("GEOFENCE_LNG", LNG)
            intent.putExtra("GEOFENCE_RADIUS", GEOFENCE_RADIUS_IN_METERS)
            ContextCompat.startForegroundService(this@MainActivity, intent)

        }

//        stopService.setOnClickListener {
//            Toast.makeText(this@MainActivity, "Service Stopped!", Toast.LENGTH_SHORT).show()
//            stopService(Intent(this@MainActivity, BackgroundService::class.java))
//        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap

        val bundle: Bundle = intent.extras!!
        LAT = bundle.get("GEOFENCE_LAT") as Double
        LNG = bundle.get("GEOFENCE_LNG") as Double
        GEOFENCE_RADIUS_IN_METERS = bundle.get("GEOFENCE_RADIUS") as Int

        AREA_LANDMARKS[GEOFENCE_ID_ENA_VILLAGE] = LatLng(LAT, LNG)

        LATLNG_ENA_VILLAGE = AREA_LANDMARKS[GEOFENCE_ID_ENA_VILLAGE]!!
        googleMap.addMarker(MarkerOptions().position(LATLNG_ENA_VILLAGE).title("Ena Village"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LATLNG_ENA_VILLAGE, 17f))

        addCircle()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        googleMap.isMyLocationEnabled = true
    }

    override fun onResume() {
        super.onResume()

//        val bundle: Bundle = intent.extras!!
//        LAT = bundle.get("GEOFENCE_LAT") as Double
//        LNG = bundle.get("GEOFENCE_LNG") as Double
//        GEOFENCE_RADIUS_IN_METERS = bundle.get("GEOFENCE_RADIUS") as Int
//
//        AREA_LANDMARKS[GEOFENCE_ID_ENA_VILLAGE] = LatLng(LAT, LNG)
//
//        LATLNG_ENA_VILLAGE = AREA_LANDMARKS[GEOFENCE_ID_ENA_VILLAGE]!!

//        addCircle()


        Log.d("ADD_CIRCLE", "onResume: ")
        registerReceiver(receiver, IntentFilter(BackgroundService.ACTION_CONNECTED))
        registerReceiver(receiver, IntentFilter(BackgroundService.ACTION_LOCATION))
    }
}