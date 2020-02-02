package com.takas125.cameraxsample.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.takas125.cameraxsample.R
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private val REQUEST_CODE_PERMISSION = 10
    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        camera_open_button.setOnClickListener {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
            if (allPermissionsGranted()) {
                val transaction = fragmentManager?.beginTransaction()
                transaction?.replace(R.id.fragment_container, CameraFragment())
                transaction?.addToBackStack(null)
                transaction?.commit()
            }

        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                val transaction = fragmentManager?.beginTransaction()
                transaction?.replace(R.id.fragment_container, CameraFragment())
                transaction?.addToBackStack(null)
                transaction?.commit()
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = context?.let {context ->
        REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it) == PackageManager.PERMISSION_GRANTED
        }
    }?: false
}