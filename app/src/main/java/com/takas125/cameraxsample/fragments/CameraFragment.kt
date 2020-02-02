package com.takas125.cameraxsample.fragments

import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.takas125.cameraxsample.MainActivity
import com.takas125.cameraxsample.R
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var outputDirectory: File
    private var lensFacing = CameraX.LensFacing.BACK
    private lateinit var mainExecutor: Executor

    private var imageCapture: ImageCapture? = null
    private var displayId = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewFinder = view_camera
        container = view as ConstraintLayout

        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview)

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls and bind all camera use cases
            updateCameraUi()
//            bindCameraUseCases()

            // 一旦コメントアウト
            // In the background, load latest photo taken (if any) for gallery thumbnail
//            lifecycleScope.launch(Dispatchers.IO) {
//                outputDirectory.listFiles { file ->
//                    EXTENSION_WHITELIST.contains(file.extension.toUpperCase())
//                }?.max()?.let { setGalleryThumbnail(it) }
//            }
        }
    }

    private fun updateCameraUi() {
        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // Listener for button used to capture photo
        camera_capture_button.setOnClickListener{
            imageCapture?.let { imageCapture ->

                // Create output file to hold the image
                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

                // Setup image capture metadata
                val metadata = ImageCapture.Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
                }

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(photoFile, metadata, mainExecutor, imageSavedListener)

            }
        }

        // 前面切り替えは一旦しない
        // Listener for button used to switch cameras
//        controls.findViewById<ImageButton>(R.id.camera_switch_button).setOnClickListener {
//            lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
//                CameraX.LensFacing.BACK
//            } else {
//                CameraX.LensFacing.FRONT
//            }
//            try {
//                // Only bind use cases if we can query a camera with this orientation
//                CameraX.getCameraWithLensFacing(lensFacing)
//
//                // Unbind all use cases and bind them again with the new lens facing configuration
//                CameraX.unbindAll()
//                bindCameraUseCases()
//            } catch (exc: Exception) {
//                // Do nothing
//            }
//        }

        // これも一旦しない
        // Listener for button used to view last photo
//        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
//            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
//                CameraFragmentDirections.actionCameraToGallery(outputDirectory.absolutePath))
//        }
    }

    /** Define callback that will be triggered after a photo has been taken and saved to disk */
    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onError(
            error: ImageCapture.ImageCaptureError, message: String, exc: Throwable?) {
            Log.e(TAG, "Photo capture failed: $message")
            exc?.printStackTrace()
        }

        override fun onImageSaved(photoFile: File) {
            Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")

            // If the folder selected is an external media directory, this is unnecessary
            // but otherwise other apps will not be able to access our images unless we
            // scan them using [MediaScannerConnection]
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(photoFile.extension)
            MediaScannerConnection.scanFile(
                context, arrayOf(photoFile.absolutePath), arrayOf(mimeType), null)
        }
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)
    }
}