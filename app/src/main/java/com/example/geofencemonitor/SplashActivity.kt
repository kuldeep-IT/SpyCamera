package com.example.geofencemonitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.example.geofencemonitor.Model.GeofenceModel
import com.example.geofencemonitor.ViewModel.MainViewModel

class SplashActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_PERMISSION_CODE = 101

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (checkInternetConnectivity()) {

            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) ||

                (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) ||

                (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), REQUEST_LOCATION_PERMISSION_CODE
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_LOCATION_PERMISSION_CODE
                    )
                } else {
                    getGeoFence()
                }
            }
        } else {
            Toast.makeText(this, "Please check your internet connection..!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getGeoFence() {
        viewModel.getGeofence(applicationContext) {
            val rm: GeofenceModel = it
            startMainActivity(rm)
        }
    }

    private fun startMainActivity(rm: GeofenceModel) {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)

        // the lat and long of : Peerbits
//        intent.putExtra("GEOFENCE_LAT", 22.9936687)
//        intent.putExtra("GEOFENCE_LNG", 72.501467)
//        intent.putExtra("GEOFENCE_RADIUS", rm.data.radius)

        // the lat and long of : Ena
//        intent.putExtra("GEOFENCE_LAT", 21.0961796)
//        intent.putExtra("GEOFENCE_LNG", 73.0383491)
//        intent.putExtra("GEOFENCE_RADIUS", rm.data.radius)

        // the lat and long of : API
        intent.putExtra("GEOFENCE_LAT", rm.data.latitude)
        intent.putExtra("GEOFENCE_LNG", rm.data.longitude)
        intent.putExtra("GEOFENCE_RADIUS", rm.data.radius)

        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) &&

                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) &&

                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                REQUEST_LOCATION_PERMISSION_CODE
                            )
                        } else {
                            getGeoFence()
                        }
                    }
                } else {
                    Toast.makeText(this, "Permission Denied..!", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    fun checkInternetConnectivity(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
    }
}