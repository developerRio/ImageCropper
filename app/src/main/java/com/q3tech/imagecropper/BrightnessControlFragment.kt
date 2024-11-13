package com.q3tech.imagecropper

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.q3tech.imagecropper.databinding.FragmentBrightnessControlBinding

private const val TAG = "BrightnessControlFragme"
class BrightnessControlFragment : Fragment() {

    private lateinit var binding: FragmentBrightnessControlBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBrightnessControlBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        triggerFullBrightness() // Elevated brightness value
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedDispatcher)

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.handleOnBackPressed()
        }

        Glide.with(this).load(
            "https://images.unsplash.com/photo-1729984283070-585dd14cb33b?q=80&w=2574&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        ).into(binding.imagePreview)

    }

    override fun onResume() {
        super.onResume()
        keepScreenAwake(true)
    }

    override fun onPause() {
        super.onPause()
        keepScreenAwake(false)
    }

    /** [keepScreenAwake] - keeps the screen awake based on true/false, use it as true inside onResume & false in onPause */
    private fun Fragment.keepScreenAwake(keepAwake: Boolean) {
        if (keepAwake) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** [setNormalBrightness] - removes flags to set normal brightness */
    private fun setNormalBrightness() {
        val layout: WindowManager.LayoutParams? = activity?.window?.attributes
        layout?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE  // This attr sets the brightness to auto
        activity?.window?.attributes = layout
    }

    /** [triggerFullBrightness] - sets the brightness to full & can be further adjusted between float values 0.1f to 1.0f */
    private fun triggerFullBrightness() {
        val layout: WindowManager.LayoutParams? = activity?.window?.attributes
        layout?.screenBrightness = 0.9f
        activity?.window?.attributes = layout
    }

    /** [onBackPressedDispatcher] is just a in-house dispatcher that handles back presses & allows us to call functions/actions when back button is pressed. */
    private val onBackPressedDispatcher = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            setNormalBrightness()
            Log.e(TAG, "handleOnBackPressed: called")
            findNavController().navigateUp()
        }
    }

}