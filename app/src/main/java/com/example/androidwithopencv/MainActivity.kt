package com.example.androidwithopencv

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.androidwithopencv.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class MainActivity : AppCompatActivity() {

    companion object {

        init {

            if (!OpenCVLoader.initDebug())
                Log.e("OpenCv", "Unable to load OpenCV");
            else
                Log.d("OpenCv", "OpenCV loaded");
        }

        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }

    lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    var currentImageType = Imgproc.COLOR_RGB2GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }




        binding.takePicture.setOnClickListener {
            takePhoto()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = getPreview()
            /**sanjeet code start*/
////                    val surfaceTexture = binding.viewFinder.surfaceTexture
////                    val surface = Surface(surfaceTexture)
////                    val executor = Executors.newSingleThreadExecutor()
////                    val previewSurfaceProvider = PreviewSurfaceProvider(surface, executor)
            //          preview.setSurfaceProvider(previewSurfaceProvider)

            /**sanjeet code end*/

            /*****************_____________________________*/

            /**my code code start*/
//            val surfaceProvider = SurfaceProvider { request: SurfaceRequest ->
//                val resolution: android.util.Size = request.resolution
//                binding.textureView.surfaceTexture!!.setDefaultBufferSize(resolution.width, resolution.height)
//                val surface = Surface(binding.textureView.surfaceTexture)
//                request.provideSurface(surface, ContextCompat.getMainExecutor(this),
//                    { result: Result? ->
//
////                        result.surface.
//                    })
//            preview.setSurfaceProvider(surfaceProvider)
            /**my code end*/

            // image capture (use case)
            imageCapture = getImageCapture()

            // Select back camera as a default (use case)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // image analyzer (use case)
            //expected width = 1024, expected height = 768
            val imageAnalyzer = getImageAnalyzer()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera and we are binding different otherwise we will not get the frame size as
                // we set to imageAnalyzer.setTargetResolution
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getPreview(): Preview {

        return Preview.Builder()
            .build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
    }

    private fun getImageCapture(): ImageCapture {

        return ImageCapture.Builder()
            .build()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun getImageAnalyzer(): ImageAnalysis {

        return ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(1600, 1200))
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { imageProxy ->
                    Log.d(TAG, "CameraXFormat: ${imageProxy.format}")
                    Log.d(TAG, "Req format: ${ImageFormat.YUV_420_888}")

                   val bitmap = if (ImageFormat.YUV_420_888 == imageProxy.format)
                        imageProxy.toBitmap()
                    else imageProxy.decodeBitmap()

                    Log.d("asdasfafafafss", "width :${imageProxy.width}, height: :${imageProxy.height}")

                    /** new code for transformation*/

                    val transFormedImage = prespectiveCorrectD42(bitmap)
                    bitmap?.recycle()
                    Log.d("CurrentThread", Thread.currentThread().name)

                    runOnUiThread {

                        binding.ivBitmap.setImageBitmap(transFormedImage)

                        Log.d("CurrentThreadm", Thread.currentThread().name)

                        imageProxy.close()
                    }

                })

            }

    }

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                }

            })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

//            val buffer = image.planes[0].buffer
//            val data = buffer.toByteArray()
//            val pixels = data.map { it.toInt() and 0xFF }
//            val luma = pixels.average()

            listener(image)

        }

    }

    /**
     *
     * this is the main function
     * */

    open fun prespectiveCorrectD42(bmp: Bitmap?): Bitmap? {
        val inputMat = Mat()
        val outputMat = Mat()

        Utils.bitmapToMat(bmp, inputMat)

        //check resolution 16:9 and others
        //check aspect ratio
        val inputSize = Size(inputMat.cols().toDouble(), inputMat.rows().toDouble())
        val outputSize = Size(0.0, 0.0)
        val mappingSize = Size(34.8, 28.0)
        val ptsObject: MutableList<Point> = ArrayList()
        val ptsTarget: MutableList<Point> = ArrayList()
        val ratio_w = inputMat.cols().toDouble() / 1280.0
        val ratio_h = inputMat.rows().toDouble() / 720.0
        ptsObject.add(Point(1250.0 * ratio_w, 720.0 * ratio_h))
        ptsObject.add(Point(0.0 * ratio_w, 720.0 * ratio_h))
        ptsObject.add(Point(350.0 * ratio_w, 100.0 * ratio_h))
        ptsObject.add(Point(958.0 * ratio_w, 100.0 * ratio_h))
        if (inputSize.width.toFloat() / inputSize.height.toFloat() > mappingSize.width / mappingSize.height) {
            outputSize.width = inputSize.width
            outputSize.height = (inputSize.width * (mappingSize.width.toFloat() / mappingSize.height.toFloat()))
        } else {
            outputSize.width = inputSize.height * (mappingSize.width.toFloat() / mappingSize.height.toFloat())
            outputSize.height = inputSize.height
        }
        ptsTarget.add(Point(0.0, 0.0))
        ptsTarget.add(Point(outputSize.width, 0.0))
        ptsTarget.add(Point(outputSize.width, outputSize.height))
        ptsTarget.add(Point(0.0, outputSize.height))

        val srcMat = Converters.vector_Point2f_to_Mat(ptsObject)
        val dstMat = Converters.vector_Point2f_to_Mat(ptsTarget)
        val transform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        Imgproc.warpPerspective(inputMat, outputMat, transform, outputSize)
        val outputBmp = Bitmap.createBitmap(outputSize.width.toInt(), outputSize.height.toInt(), RGB_565)
        Utils.matToBitmap(outputMat, outputBmp)
        return outputBmp
    }



}

typealias LumaListener = (image: ImageProxy) -> Unit