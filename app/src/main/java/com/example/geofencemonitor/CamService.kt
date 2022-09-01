package com.example.geofencemonitor

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.absoluteValue
import android.graphics.ImageFormat

import android.graphics.YuvImage
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import androidx.palette.graphics.Palette
import com.example.geofencemonitor.Model.NotifyValueModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.HashSet
import android.hardware.camera2.CameraCharacteristics
import com.example.geofencemonitor.Model.ImageConfigurationModel


class CamService : Service() {

    // UI
    private var wm: WindowManager? = null
    private var rootView: View? = null
    private var textureView: TextureView? = null

    // Camera2-related stuff
    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    // You can start service in 2 modes - 1.) with preview 2.) without preview (only bg processing)
    private var shouldShowPreview = true

    var notificationChannel: NotificationChannel? = null
    var notificationManager: NotificationManager? = null
    var builder: Notification.Builder? = null

    private val channelId = "12345"
    private val description = "Test Notification"


    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            initCam(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }


    private val imageListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image? = reader?.acquireLatestImage()

        Log.d(TAG, "Got image: " + image?.width + " x " + image?.height)
//        Toast.makeText(applicationContext, "Image captured..!", Toast.LENGTH_SHORT).show()

        if (image != null) {

            val data: ByteArray? = NV21toJPEG(
                YUV_420_888toNV21(image)!!,
                image.width, image.height
            )

            try {
                val x = System.currentTimeMillis()
                val outStream =
                    FileOutputStream(commonDocumentDirPath("camera2")!!.path + "/camera2image" + x + ".jpeg")
                outStream.write(data)
                outStream.close()

                val bitMapImage = BitmapFactory.decodeByteArray(data, 0, data!!.size)
                getColorsFromBitMap(bitMapImage)
            } catch (e: IOException) {
                Log.d("CAMERA", e.message!!)
            }
            image.close()
        }
    }

    private fun getColorsFromBitMap(bitMapImage: Bitmap) {

        val colorsArray = ArrayList<String>()

        for (y in 0 until bitMapImage.height) {
            for (x in 0 until bitMapImage.width) {
                val pixel = bitMapImage.getPixel(x, y)
                colorsArray.add("#" + Integer.toHexString(pixel))
            }
        }
        val hashSet = HashSet<String>()
        hashSet.addAll(colorsArray)
        colorsArray.clear()
        colorsArray.addAll(hashSet)
        Log.e("colorsArray", "" + colorsArray)

        getNotifyValue(colorsArray, applicationContext) {
            val nvm: NotifyValueModel = it
//            Toast.makeText(
//                applicationContext,
//                "notify_value: " + nvm.data.notify_value,
//                Toast.LENGTH_SHORT
//            ).show()

            if (it.data.notify_value) {
                locationLeftNotify()
            }
        }
    }

    //API call for get notify value start
    fun getNotifyValue(
        colorsArray: ArrayList<String>,
        context: Context,
        handleResponse: (response: NotifyValueModel) -> Unit
    ) {
        val call: Call<NotifyValueModel> = RetrofitClient.instance!!.myApi.getNotifyValue()
        call.enqueue(object : Callback<NotifyValueModel> {
            override fun onResponse(
                call: Call<NotifyValueModel>,
                response: Response<NotifyValueModel>
            ) {
                handleResponse(response.body()!!)
            }

            override fun onFailure(call: Call<NotifyValueModel>, t: Throwable?) {
//                Toast.makeText(context, "An error has occured", Toast.LENGTH_LONG).show()
            }
        })
    }
    //API call for get notify value end

    private fun locationLeftNotify() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, SplashActivity::class.java)


        var pendingIntent : PendingIntent? =null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_MUTABLE)
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

//        val pendingIntent =
//            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel!!.setLightColor(Color.BLUE)
            notificationChannel!!.enableVibration(true)
            notificationManager!!.createNotificationChannel(notificationChannel!!)
            builder = Notification.Builder(this, channelId).setContentTitle("Alert")
                .setContentText("Location Left")
                .setSmallIcon(R.drawable.ic_launcher_foreground) //                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher_background))
                .setContentIntent(pendingIntent)
        }
        notificationManager!!.notify(12345, builder!!.build())
    }

    private fun YUV_420_888toNV21(image: Image): ByteArray? {
        val nv21: ByteArray
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        return nv21
    }

    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return out.toByteArray()
    }

    fun commonDocumentDirPath(FolderName: String): File? {
        var dir: File?
        dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString() + "/" + FolderName
            )
        } else {
            File(Environment.getExternalStorageDirectory().toString() + "/" + FolderName)
        }
        if (!dir.exists()) {
            val success = dir.mkdirs()
            if (!success) {
                dir = null
            }
        }
        return dir
    }

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            cameraDevice = currentCameraDevice
            createCaptureSession()
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            currentCameraDevice.close()
            cameraDevice = null
        }

        override fun onError(currentCameraDevice: CameraDevice, error: Int) {
            currentCameraDevice.close()
            cameraDevice = null
        }
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_START -> start()

            ACTION_START_WITH_PREVIEW -> startWithPreview()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopCamera()

        if (rootView != null)
            wm?.removeView(rootView)

        sendBroadcast(Intent(ACTION_STOPPED))
    }

    private fun start() {

        shouldShowPreview = false

        initCam(320, 200)
    }

    private fun startWithPreview() {

        shouldShowPreview = true

        // Initialize view drawn over other apps
        initOverlay()

        // Initialize camera here if texture view already initialized
        if (textureView!!.isAvailable)
            initCam(textureView!!.width, textureView!!.height)
        else
            textureView!!.surfaceTextureListener = surfaceTextureListener
    }

    private fun initOverlay() {

        val li = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        rootView = li.inflate(R.layout.overlay, null)
        textureView = rootView?.findViewById(R.id.texPreview)

        val type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm!!.addView(rootView, params)
    }

    private fun initCam(width: Int, height: Int) {

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var camId: String? = null

        for (id in cameraManager!!.cameraIdList) {
            val characteristics = cameraManager!!.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                camId = id
                break
            }
        }

        previewSize = chooseSupportedSize(camId!!, width, height)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        cameraManager!!.openCamera(camId, stateCallback, null)
    }

    private fun chooseSupportedSize(
        camId: String,
        textureViewWidth: Int,
        textureViewHeight: Int
    ): Size {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get all supported sizes for TextureView
        val characteristics = manager.getCameraCharacteristics(camId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = map!!.getOutputSizes(SurfaceTexture::class.java)

        // We want to find something near the size of our TextureView
        val texViewArea = textureViewWidth * textureViewHeight
        val texViewAspect = textureViewWidth.toFloat() / textureViewHeight.toFloat()

        val nearestToFurthestSz = supportedSizes.sortedWith(compareBy(
            // First find something with similar aspect
            {
                val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                else it.height.toFloat() / it.width.toFloat()
                (aspect - texViewAspect).absoluteValue
            },
            // Also try to get similar resolution
            {
                (texViewArea - it.width * it.height).absoluteValue
            }
        ))


        if (nearestToFurthestSz.isNotEmpty())
            return nearestToFurthestSz[0]

        return Size(320, 200)
    }

    private fun startForeground() {



        var pendingIntent: PendingIntent? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent,PendingIntent.FLAG_MUTABLE)
                }
//            pendingIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_MUTABLE)
        } else {
            pendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }
//            pendingIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_ONE_SHOT)
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.app_name))
            .setSmallIcon(R.drawable.notification_template_icon_bg)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.app_name))
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createCaptureSession() {
        try {
            // Prepare surfaces we want to use in capture session
            val targetSurfaces = ArrayList<Surface>()

            // Prepare CaptureRequest that can be used with CameraCaptureSession
            val requestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {

                    if (shouldShowPreview) {
                        val texture = textureView!!.surfaceTexture!!
                        texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
                        val previewSurface = Surface(texture)

                        targetSurfaces.add(previewSurface)
                        addTarget(previewSurface)
                    }

                    // Configure target surface for background processing (ImageReader)
                    imageReader = ImageReader.newInstance(
                        previewSize!!.width, previewSize!!.height,
                        ImageFormat.YUV_420_888, 2
                    )
                    imageReader!!.setOnImageAvailableListener(imageListener, null)

                    targetSurfaces.add(imageReader!!.surface)
                    addTarget(imageReader!!.surface)

                    // Set some additional parameters for the request
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )


                    getImageConfigurations(applicationContext) {
                        val icm: ImageConfigurationModel = it

                        val zoom_value = icm.data.zoom_value.toDouble()
//                        val zoom_value = 2.0
                        val photo_mode = icm.data.photo_mode.toInt()
//                        val photo_mode = CaptureRequest.CONTROL_SCENE_MODE_SUNSET

                        val mCropRegion = Rect()
                        val DEFAULT_ZOOM_FACTOR = 1.0;
                        var mSensorSize: Rect? = null
                        var modes: IntArray? = null

                        for (id in cameraManager!!.cameraIdList) {
                            val characteristics = cameraManager!!.getCameraCharacteristics(id)
                            mSensorSize =
                                characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                            modes =
                                characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                        }

                        val newZoom: Double = MathUtils.clamp(zoom_value, DEFAULT_ZOOM_FACTOR, 5.0)

                        val centerX: Int = mSensorSize!!.width() / 2;
                        val centerY: Int = mSensorSize.height() / 2;
                        val deltaX: Int = ((0.5f * mSensorSize.width()) / newZoom).toInt();
                        val deltaY: Int = ((0.5f * mSensorSize.height()) / newZoom).toInt();

                        mCropRegion.set(
                            centerX - deltaX,
                            centerY - deltaY,
                            centerX + deltaX,
                            centerY + deltaY
                        )

                        set(CaptureRequest.SCALER_CROP_REGION, mCropRegion)


                        for (mode in modes!!) {
                            Log.e("modes", mode.toString())
                        }
                        if (modes.contains(photo_mode)) {
                            set(
                                CaptureRequest.CONTROL_SCENE_MODE,
                                photo_mode
                            )
                        }
                    }
                }

            // Prepare CameraCaptureSession
            cameraDevice!!.createCaptureSession(
                targetSurfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }

                        captureSession = cameraCaptureSession
                        try {
                            // Now we can start capturing
                            captureRequest = requestBuilder.build()
                            captureSession!!.setRepeatingRequest(
                                captureRequest!!,
                                captureCallback,
                                null
                            )
                            captureSession!!.stopRepeating()

//                            val singleRequest = captureSession!!.device.createCaptureRequest(
//                                CameraDevice.TEMPLATE_STILL_CAPTURE)
//                            singleRequest.addTarget(targetSurfaces[0])
//                            captureSession!!.capture(singleRequest.build(), null, null)

                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "createCaptureSession", e)
                        }

                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Log.e(TAG, "createCaptureSession()")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "createCaptureSession", e)
        }
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
                Toast.makeText(context, "An error has occured", Toast.LENGTH_LONG).show()
            }
        })
    }
    //API call for image configuration end

    private fun stopCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {

        val TAG = "CamService"

        val ACTION_START = "eu.sisik.backgroundcam.action.START"
        val ACTION_START_WITH_PREVIEW = "eu.sisik.backgroundcam.action.START_WITH_PREVIEW"
        val ACTION_STOPPED = "eu.sisik.backgroundcam.action.STOPPED"

        val ONGOING_NOTIFICATION_ID = 6660
        val CHANNEL_ID = "cam_service_channel_id"
        val CHANNEL_NAME = "cam_service_channel_name"

    }
}