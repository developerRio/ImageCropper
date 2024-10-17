package com.q3tech.imagecropper

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.q3tech.imagecropper.cropper.CropImageContract
import com.q3tech.imagecropper.cropper.CropImageContractOptions
import com.q3tech.imagecropper.cropper.CropImageOptions
import com.q3tech.imagecropper.cropper.CropImageView
import com.q3tech.imagecropper.databinding.ActivityMainBinding

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.pickImageButton.setOnClickListener {
            startOpsFromLib()
        }
    }

    private fun startOpsFromLib() {
        cropImage.launch(
            CropImageContractOptions(
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    minCropResultWidth = 450,
                    minCropResultHeight = 650,
                    maxCropResultWidth = 900,
                    maxCropResultHeight = 1300,
                    cropShape = CropImageView.CropShape.OVAL,
                    showCropLabel = true,
                    showCropOverlay = true,
                    allowRotation = true
                ),
                uri = Uri.EMPTY
            )
        )
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedImageUri = result.uriContent
            val croppedImageFilePath = result.getUriFilePath(this)
            Log.e(TAG, "croppedImageUri: $croppedImageUri")
            Log.e(TAG, "croppedImageFilePath: $croppedImageFilePath")
            binding.imagePreview.setImageURI(croppedImageUri)
        } else {
            val exception = result.error
            Log.e(TAG, "exception: ${exception?.message}")

        }
    }

}