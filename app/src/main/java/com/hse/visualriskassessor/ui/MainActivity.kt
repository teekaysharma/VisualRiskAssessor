package com.hse.visualriskassessor.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.hse.visualriskassessor.BuildConfig
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.ui.camera.CameraActivity
import com.hse.visualriskassessor.ui.history.HistoryActivity
import com.hse.visualriskassessor.ui.results.ResultsActivity
import com.hse.visualriskassessor.utils.PermissionManager

class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            showPermissionDeniedDialog("Camera")
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            openPhotoPicker()
        } else {
            showPermissionDeniedDialog("Storage")
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val intent = Intent(this, ResultsActivity::class.java).apply {
                putExtra(ResultsActivity.EXTRA_IMAGE_URI, it.toString())
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionManager = PermissionManager(this)
        setupViews()
    }

    private fun setupViews() {
        findViewById<MaterialButton>(R.id.btnTakePhoto).setOnClickListener {
            handleTakePhoto()
        }

        findViewById<MaterialButton>(R.id.btnChoosePhoto).setOnClickListener {
            handleChoosePhoto()
        }

        findViewById<MaterialCardView>(R.id.historyCard).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.aboutCard).setOnClickListener {
            showAboutDialog()
        }
    }

    private fun handleTakePhoto() {
        when {
            permissionManager.hasCameraPermission() -> {
                openCamera()
            }
            permissionManager.shouldShowRationale(
                PermissionManager.CAMERA_PERMISSION,
                this
            ) -> {
                showPermissionRationaleDialog(
                    "Camera",
                    "Camera access is needed to capture workplace images for risk assessment."
                ) {
                    permissionManager.requestCameraPermission(cameraPermissionLauncher)
                }
            }
            else -> {
                permissionManager.requestCameraPermission(cameraPermissionLauncher)
            }
        }
    }

    private fun handleChoosePhoto() {
        when {
            permissionManager.hasStoragePermission() -> {
                openPhotoPicker()
            }
            else -> {
                permissionManager.requestStoragePermission(storagePermissionLauncher)
            }
        }
    }

    private fun openCamera() {
        startActivity(Intent(this, CameraActivity::class.java))
    }

    private fun openPhotoPicker() {
        photoPickerLauncher.launch("image/*")
    }

    private fun showPermissionRationaleDialog(
        permissionName: String,
        message: String,
        onPositive: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle("$permissionName Permission Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ -> onPositive() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedDialog(permissionName: String) {
        AlertDialog.Builder(this)
            .setTitle("$permissionName Permission Denied")
            .setMessage("$permissionName permission is required for this feature. You can enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                permissionManager.openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        val version = BuildConfig.VERSION_NAME
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(getString(R.string.about_description) + "\n\n" +
                    getString(R.string.about_version, version) + "\n\n" +
                    getString(R.string.about_disclaimer))
            .setPositiveButton("OK", null)
            .show()
    }
}
