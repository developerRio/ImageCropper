package com.q3tech.imagecropper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
   //private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
   //private lateinit var cropImageLauncher: ActivityResultLauncher<CropImageContractOptions>

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

        /*pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    startCrop(imageUri)
                }
            }
        }

        cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                val croppedUri = result.uriContent
                binding.imagePreview.setImageURI(croppedUri)
            } else {
                val error = result.error
                Log.e(TAG, "onCreate: $error")
            }
        }*/

        binding.pickImageButton.setOnClickListener {
            startOpsFromLib()
            //startOpsFromRaw()
        }


    }

    /*private fun startCrop(uri: Uri) {
        Log.e(TAG, "startCrop: $uri")
        cropImageLauncher.launch(CropImageContractOptions(uri, CropImageOptions(
            showCropLabel = true,
            showCropOverlay = true,
            allowRotation = true
        )))
    }*/

    private fun startOpsFromLib() {
        cropImage.launch(
            CropImageContractOptions(
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    minCropResultWidth = 500,
                    minCropResultHeight = 650,
                    maxCropResultWidth = 500,
                    maxCropResultHeight = 650,
                    cropShape = CropImageView.CropShape.OVAL,
                    showCropLabel = true,
                    showCropOverlay = true,
                    allowRotation = true
                ),
                uri = Uri.EMPTY
            )
        )
    }

    /*private fun startOpsFromRaw() {
        openGallery()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pickImageLauncher.launch(intent)
    }*/

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