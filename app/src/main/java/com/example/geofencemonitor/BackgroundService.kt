package com.example.geofencemonitor

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.geofencemonitor.Model.ImageConfigurationModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BackgroundService : Service(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    var mGoogleApiClient: GoogleApiClient? = null
    var mLocationCallbacks: LocationCallback? = null
    var mLocationRequest: LocationRequest? = null
    var audioManager: AudioManager? = null

    var LAT: Double = 0.0
    var LNG: Double = 0.0
    var GEOFENCE_RADIUS_IN_METERS: Int = 0

    val CHANNEL_ID = "backgroundServiceChannel"

    val ZOOM_VALUE = "ZOOM_VALUE"
    val PHOTO_MODE = "PHOTO_MODE"

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnected(p0: Bundle?) {

        sendBroadcast(Intent(ACTION_CONNECTED))

        mLocationRequest = LocationRequest.create()
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest!!.interval = 10

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
                mLocationRequest!!, mLocationCallbacks!!, null
            )
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = baseContext.getSystemService(AUDIO_SERVICE) as AudioManager

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        mLocationCallbacks = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                val latitude = location!!.latitude
                val longitude = location.longitude

                val intent = Intent(ACTION_LOCATION)
                intent.putExtra("CURRENT_LAT", latitude)
                intent.putExtra("CURRENT_LNG", longitude)
                sendBroadcast(intent)

                if (isInCampus(latitude, longitude)) {

                    manageCameraService("In")

//                    Toast.makeText(
//                        applicationContext,
//                        "Inside\n Current location(Lat: $latitude, Lng: $longitude)",
//                        Toast.LENGTH_SHORT
//                    ).show()
                } else {

                    manageCameraService("Out")

//                    Toast.makeText(
//                        applicationContext,
//                        "Outside\n Current location(Lat: $latitude, Lng: $longitude)",
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
            }
        }
    }

    private fun manageCameraService(msg: String) {
        Log.e("Msg", msg)
        if (msg == "In") {
//            Toast.makeText(this.applicationContext, "Capturing stop", Toast.LENGTH_SHORT).show()
            if (isServiceRunning(this, CamService::class.java)) {
                stopService(Intent(this, CamService::class.java))
            }
        } else {
            getImageConfigurations()
        }
    }

    private fun getImageConfigurations() {
        getImageConfigurations(applicationContext) {
            val icm: ImageConfigurationModel = it

            if (!isServiceRunning(this, CamService::class.java)) {
                notifyService(CamService.ACTION_START, icm)
            }
        }
    }

    private fun notifyService(action: String, icm: ImageConfigurationModel) {
        val intent = Intent(this, CamService::class.java)
        intent.action = action
        intent.putExtra(ZOOM_VALUE, icm.data.zoom_value)
        intent.putExtra(PHOTO_MODE, icm.data.photo_mode)
        startService(intent)
    }

    //API call for image configuration start
    fun getImageConfigurations(
        context: Context,
        handleResponse: (response: ImageConfigurationModel) -> Unit
    ) {
        val call: Call<ImageConfigurationModel> =
            RetrofitClient.instance!!.myApi.getImageConfiguration()
        call.enqueue(object : Callback<ImageConfigurationModel> {
            override fun onResponse(
                call: Call<ImageConfigurationModel>,
                response: Response<ImageConfigurationModel>
            ) {
                handleResponse(response.body()!!)
            }

            override fun onFailure(call: Call<ImageConfigurationModel>, t: Throwable?) {
//                Toast.makeText(context, "An error has occured", Toast.LENGTH_LONG).show()
            }
        })
    }
    //API call for image configuration end

    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371
        val latDistance = Math.toRadians(Math.abs(lat2 - lat1))
        val lonDistance = Math.toRadians(Math.abs(lon2 - lon1))
        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        var distance = R * c * 1000
        distance = Math.pow(distance, 2.0)
        return Math.sqrt(distance)
    }

    private fun isInCampus(x: Double, y: Double): Boolean {
        val X = LAT
        val Y = LNG

        return getDistance(X, Y, x, y) <= GEOFENCE_RADIUS_IN_METERS
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        intent.let {
        if(intent !== null){
            LAT =  intent!!.getDoubleExtra("GEOFENCE_LAT", 0.0)
            LNG =  intent!!.getDoubleExtra("GEOFENCE_LNG", 0.0)
            GEOFENCE_RADIUS_IN_METERS = intent!!.getIntExtra("GEOFENCE_RADIUS", 0)
//            val notificationIntent = Intent(this, MainActivity::class.java)
//            val listOfIntents = arrayOfNulls<Intent>(1)
//            listOfIntents[0] = notificationIntent
//            val pendingIntent = PendingIntent.getActivities(
//                this, 0, listOfIntents, 0
//            )

            val intent1 = Intent(this, SplashActivity::class.java)
//            intent1.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK )
//
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            var pendingIntent : PendingIntent? =null

             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                 pendingIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_MUTABLE)
            } else {
                 pendingIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_ONE_SHOT)
            }

//            val pendingIntent = PendingIntent.getActivity(this, 0, intent1,
//                0)

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Service Active")
                .setContentText("Tap to return")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)

            if (mGoogleApiClient != null) {
                mGoogleApiClient!!.connect()
            }

//            return START_STICKY
        }
        return START_STICKY
//        LAT = intent!!.getDoubleExtra("GEOFENCE_LAT", 0.0)
//        LNG = intent.getDoubleExtra("GEOFENCE_LNG", 0.0)
//        GEOFENCE_RADIUS_IN_METERS = intent.getIntExtra("GEOFENCE_RADIUS", 0)

        /*val notificationIntent = Intent(this, MainActivity::class.java)
        val listOfIntents = arrayOfNulls<Intent>(1)
        listOfIntents[0] = notificationIntent
        val pendingIntent = PendingIntent.getActivities(
            this, 0, listOfIntents, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Background Service Active")
            .setContentText("Tap to return")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.connect()
        }

        return START_STICKY
        */
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mGoogleApiClient!!.isConnected()) {
            mGoogleApiClient!!.disconnect();
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        val ACTION_CONNECTED = "action.CONNECTED"
        val ACTION_LOCATION = "action.LOCATION"
    }
}