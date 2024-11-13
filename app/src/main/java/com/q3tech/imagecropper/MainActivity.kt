package com.q3tech.imagecropper

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.q3tech.imagecropper.databinding.ActivityMainBinding

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*binding.pickImageButton.setOnClickListener {
            startOpsFromLib()
        }*/
    }

    /*private fun startOpsFromLib() {
        cropImage.launch(
            CropImageContractOptions(
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    *//*minCropResultWidth = 450,
                    minCropResultHeight = 650,
                    maxCropResultWidth = 900,
                    maxCropResultHeight = 1300,*//*
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
            //binding.imagePreview.setImageURI(croppedImageUri)
        } else {
            val exception = result.error
            Log.e(TAG, "exception: ${exception?.message}")

        }
    }*/

}