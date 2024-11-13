package com.q3tech.imagecropper

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.q3tech.imagecropper.cropper.CropImageContract
import com.q3tech.imagecropper.cropper.CropImageContractOptions
import com.q3tech.imagecropper.cropper.CropImageOptions
import com.q3tech.imagecropper.cropper.CropImageView
import com.q3tech.imagecropper.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    private val TAG = "MainFragment"

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val croppedImageFilePath = result.getUriFilePath(requireActivity())
            Log.e(TAG, "croppedImageUri: $croppedImageUri")
            Log.e(TAG, "croppedImageFilePath: $croppedImageFilePath")
            binding.imagePreview.setImageURI(croppedImageUri)
        } else {
            val exception = result.error
            Log.e(TAG, "exception: ${exception?.message}")

        }
    }

}