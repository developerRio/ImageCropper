package com.q3tech.imagecropper.cropper

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.q3tech.imagecropper.R
import com.q3tech.imagecropper.cropper.CropImageView.CropResult
import com.q3tech.imagecropper.cropper.CropImageView.OnCropImageCompleteListener
import com.q3tech.imagecropper.cropper.CropImageView.OnSetImageUriCompleteListener
import com.q3tech.imagecropper.cropper.utils.getUriForFile
import com.q3tech.imagecropper.databinding.CropImageActivityBinding
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val TAG = "CropImageActivity"
open class CropImageActivity :
  AppCompatActivity(),
  OnSetImageUriCompleteListener,
  OnCropImageCompleteListener {

  /** Persist URI image to crop URI if specific permissions are required. */
  private var cropImageUri: Uri? = null

  /** The options that were set for the crop image*/
  private lateinit var cropImageOptions: CropImageOptions

  /** The crop image view library widget used in the activity. */
  private var cropImageView: CropImageView? = null
  private lateinit var binding: CropImageActivityBinding
  private var latestTmpUri: Uri? = null
  private val pickImageGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    onPickImageResult(uri)
  }

  private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
    if (it) {
      onPickImageResult(latestTmpUri)
    } else {
      onPickImageResult(null)
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = CropImageActivityBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setCropImageView(binding.cropImageView)
    val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
    cropImageUri = bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
    cropImageOptions =
      bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS) ?: CropImageOptions()

    if (savedInstanceState == null) {
      if (cropImageUri == null || cropImageUri == Uri.EMPTY) {
        when {
          cropImageOptions.showIntentChooser -> showIntentChooser()
          cropImageOptions.imageSourceIncludeGallery &&
            cropImageOptions.imageSourceIncludeCamera ->
            showImageSourceDialog(::openSource)
          cropImageOptions.imageSourceIncludeGallery ->
            pickImageGallery.launch("image/*")
          cropImageOptions.imageSourceIncludeCamera ->
            openCamera()
          else -> finish()
        }
      } else {
        cropImageView?.setImageUriAsync(cropImageUri)
      }
    } else {
      latestTmpUri = savedInstanceState.getString(BUNDLE_KEY_TMP_URI)?.toUri()
    }

    setCustomizations()

    onBackPressedDispatcher.addCallback {
      setResultCancel()
    }

    binding.rotateLeftButton.setOnClickListener {
      rotateImage(-cropImageOptions.rotationDegrees)
    }

    binding.rotateRightButton.setOnClickListener {
      rotateImage(-cropImageOptions.rotationDegrees)
    }



    binding.cropImageView.setOnSetCropOverlayReleasedListener { rect ->
      val drawable = binding.imagePreview.drawable as BitmapDrawable
      val originalBitmap = drawable.bitmap
      Log.e(TAG, "setOnSetCropOverlayReleasedListener: ${rect?.height()} ${rect?.width()} ${rect?.left} ${rect?.right} org: ${originalBitmap.width}")
      Log.e(TAG, "cropImageView uri: ${rect?.isEmpty}")
      Handler(Looper.getMainLooper()).postDelayed(Runnable {
        val croppedBitmap = getBitmapForPreview(rect, originalBitmap)
        //val cropImageUri = saveBitmapToFile(croppedBitmap)
        //setImageToPreview(cropImageUri)
        binding.imagePreview.setImageBitmap(croppedBitmap)
      }, 600)

    }
  }

  private fun getBitmapForPreview(rect: Rect?, originalBitmap: Bitmap?): Bitmap {
    Log.e(TAG, "getBitmapForPreview: ${rect?.left} ${rect?.width()} org: ${originalBitmap?.width}")
    val left = rect?.left?.coerceAtLeast(0) ?: 10
    val top = rect?.top?.coerceAtLeast(0)?: 10
    val right = rect?.right?.coerceAtMost(originalBitmap?.width ?: 10) ?: 10
    val bottom = rect?.bottom?.coerceAtMost(originalBitmap?.height ?: 10) ?: 10
    return if (originalBitmap != null && rect != null && left < right && top < bottom) {
      Bitmap.createBitmap(originalBitmap, rect.left, rect.top,  right - left, bottom - top)
    } else {
      Bitmap.createBitmap(binding.imagePreview.drawable.toBitmap(), 0,0,0,0)
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun saveBitmapToFile(bitmap: Bitmap): Uri {
    val file = File(this.cacheDir, "cropped_image_${getCurrentTimestamp()}.png")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    outputStream.flush()
    outputStream.close()
    return Uri.fromFile(file)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun getCurrentTimestamp(): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
    return current.format(formatter)
  }

  private fun setImageToPreview(croppedImageUri: Uri?) {
    if (cropImageUri != null) {
      Log.d(TAG, "onCreate: cropImageUri = $croppedImageUri")
      binding.imagePreview.setImageURI(croppedImageUri)
    } else {
      Log.e(TAG, "onCreate: cropImageUri = $croppedImageUri")
    }
  }

  private fun setCustomizations() {
    cropImageOptions.activityBackgroundColor.let { activityBackgroundColor ->
      binding.root.setBackgroundColor(activityBackgroundColor)
    }

    supportActionBar?.let {
      title = cropImageOptions.activityTitle.ifEmpty { "" }
      it.setDisplayHomeAsUpEnabled(true)
      cropImageOptions.toolbarColor?.let { toolbarColor ->
        it.setBackgroundDrawable(ColorDrawable(toolbarColor))
      }
      cropImageOptions.toolbarTitleColor?.let { toolbarTitleColor ->
        val spannableTitle: Spannable = SpannableString(title)
        spannableTitle.setSpan(
          ForegroundColorSpan(toolbarTitleColor),
          0,
          spannableTitle.length,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        title = spannableTitle
      }
      cropImageOptions.toolbarBackButtonColor?.let { backBtnColor ->
        try {
          val upArrow = ContextCompat.getDrawable(
            this,
            R.drawable.ic_arrow_back_24,
          )
          upArrow?.colorFilter = PorterDuffColorFilter(backBtnColor, PorterDuff.Mode.SRC_ATOP)
          it.setHomeAsUpIndicator(upArrow)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  private fun showIntentChooser() {
    val ciIntentChooser = CropImageIntentChooser(
      activity = this,
      callback = object : CropImageIntentChooser.ResultCallback {
        override fun onSuccess(uri: Uri?) {
          onPickImageResult(uri)
        }

        override fun onCancelled() {
          setResultCancel()
        }
      },
    )
    cropImageOptions.let { options ->
      options.intentChooserTitle
        ?.takeIf { title ->
          title.isNotBlank()
        }
        ?.let { icTitle ->
          ciIntentChooser.setIntentChooserTitle(icTitle)
        }
      options.intentChooserPriorityList
        ?.takeIf { appPriorityList -> appPriorityList.isNotEmpty() }
        ?.let { appsList ->
          ciIntentChooser.setupPriorityAppsList(appsList)
        }
      val cameraUri: Uri? = if (options.imageSourceIncludeCamera) getTmpFileUri() else null
      ciIntentChooser.showChooserIntent(
        includeCamera = options.imageSourceIncludeCamera,
        includeGallery = options.imageSourceIncludeGallery,
        cameraImgUri = cameraUri,
      )
    }
  }

  private fun openSource(source: Source) {
    when (source) {
      Source.CAMERA -> openCamera()
      Source.GALLERY -> pickImageGallery.launch("image/*")
    }
  }

  private fun openCamera() {
    getTmpFileUri().let { uri ->
      latestTmpUri = uri
      takePicture.launch(uri)
    }
  }

  private fun getTmpFileUri(): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
      createNewFile()
      deleteOnExit()
    }

    return getUriForFile(this, tmpFile)
  }

  /**
   * This method show the dialog for user source choice, it is an open function so can be overridden
   * and customised with the app layout if you need.
   */
  open fun showImageSourceDialog(openSource: (Source) -> Unit) {
    AlertDialog.Builder(this)
      .setCancelable(false)
      .setOnKeyListener { _, keyCode, keyEvent ->
        if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
          setResultCancel()
          finish()
        }
        true
      }
      .setTitle(R.string.pick_image_chooser_title)
      .setItems(
        arrayOf(
          getString(R.string.pick_image_camera),
          getString(R.string.pick_image_gallery),
        ),
      ) { _, position -> openSource(if (position == 0) Source.CAMERA else Source.GALLERY) }
      .show()
  }

  public override fun onStart() {
    super.onStart()
    cropImageView?.setOnSetImageUriCompleteListener(this)
    cropImageView?.setOnCropImageCompleteListener(this)
  }

  public override fun onStop() {
    super.onStop()
    cropImageView?.setOnSetImageUriCompleteListener(null)
    cropImageView?.setOnCropImageCompleteListener(null)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(BUNDLE_KEY_TMP_URI, latestTmpUri.toString())
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    if (cropImageOptions.skipEditing) return true
    menuInflater.inflate(R.menu.crop_image_menu, menu)

    if (!cropImageOptions.allowRotation) {
      menu.removeItem(R.id.ic_rotate_left_24)
      menu.removeItem(R.id.ic_rotate_right_24)
    } else if (cropImageOptions.allowCounterRotation) {
      menu.findItem(R.id.ic_rotate_left_24).isVisible = true
    }

    if (!cropImageOptions.allowFlipping) menu.removeItem(R.id.ic_flip_24)

    if (cropImageOptions.cropMenuCropButtonTitle != null) {
      menu.findItem(R.id.crop_image_menu_crop).title =
        cropImageOptions.cropMenuCropButtonTitle
    }

    var cropIcon: Drawable? = null
    try {
      if (cropImageOptions.cropMenuCropButtonIcon != 0) {
        cropIcon = ContextCompat.getDrawable(this, cropImageOptions.cropMenuCropButtonIcon)
        menu.findItem(R.id.crop_image_menu_crop).icon = cropIcon
      }
    } catch (e: Exception) {
      Log.w("AIC", "Failed to read menu crop drawable", e)
    }

    if (cropImageOptions.activityMenuIconColor != 0) {
      updateMenuItemIconColor(
        menu,
        R.id.ic_rotate_left_24,
        cropImageOptions.activityMenuIconColor,
      )
      updateMenuItemIconColor(
        menu,
        R.id.ic_rotate_right_24,
        cropImageOptions.activityMenuIconColor,
      )
      updateMenuItemIconColor(menu, R.id.ic_flip_24, cropImageOptions.activityMenuIconColor)

      if (cropIcon != null) {
        updateMenuItemIconColor(
          menu,
          R.id.crop_image_menu_crop,
          cropImageOptions.activityMenuIconColor,
        )
      }
    }
    cropImageOptions.activityMenuTextColor?.let { menuItemsTextColor ->
      val menuItemIds = listOf(
        R.id.ic_rotate_left_24,
        R.id.ic_rotate_right_24,
        R.id.ic_flip_24,
        R.id.ic_flip_24_horizontally,
        R.id.ic_flip_24_vertically,
        R.id.crop_image_menu_crop,
      )
      for (itemId in menuItemIds) {
        updateMenuItemTextColor(menu, itemId, menuItemsTextColor)
      }
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
    R.id.crop_image_menu_crop -> {
      cropImage()
      true
    }
    R.id.ic_rotate_left_24 -> {
      rotateImage(-cropImageOptions.rotationDegrees)
      true
    }
    R.id.ic_rotate_right_24 -> {
      rotateImage(cropImageOptions.rotationDegrees)
      true
    }
    R.id.ic_flip_24_horizontally -> {
      cropImageView?.flipImageHorizontally()
      true
    }
    R.id.ic_flip_24_vertically -> {
      cropImageView?.flipImageVertically()
      true
    }
    android.R.id.home -> {
      setResultCancel()
      true
    }
    else -> super.onOptionsItemSelected(item)
  }

  protected open fun onPickImageResult(resultUri: Uri?) {
    when (resultUri) {
      null -> setResultCancel()
      else -> {
        cropImageUri = resultUri
        cropImageView?.setImageUriAsync(cropImageUri)
        setImageToPreview(cropImageUri)
      }
    }
  }

  override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
    if (error == null) {
      if (cropImageOptions.initialCropWindowRectangle != null) {
        cropImageView?.cropRect = cropImageOptions.initialCropWindowRectangle
      }

      if (cropImageOptions.initialRotation > 0) {
        cropImageView?.rotatedDegrees = cropImageOptions.initialRotation
      }

      if (cropImageOptions.skipEditing) {
        cropImage()
      }
    } else {
      setResult(null, error, 1)
    }
  }

  override fun onCropImageComplete(view: CropImageView, result: CropResult) {
    setResult(result.uriContent, result.error, result.sampleSize)
    Log.d(TAG, "onCropImageComplete: ${result.uriContent}")
  }

  /**
   * Execute crop image and save the result tou output uri.
   */
  open fun cropImage() {
    if (cropImageOptions.noOutputImage) {
      setResult(null, null, 1)
    } else {
      cropImageView?.croppedImageAsync(
        saveCompressFormat = cropImageOptions.outputCompressFormat,
        saveCompressQuality = cropImageOptions.outputCompressQuality,
        reqWidth = cropImageOptions.outputRequestWidth,
        reqHeight = cropImageOptions.outputRequestHeight,
        options = cropImageOptions.outputRequestSizeOptions,
        customOutputUri = cropImageOptions.customOutputUri,
      )
    }
  }

  /**
   * When extending this activity, please set your own ImageCropView
   */
  open fun setCropImageView(cropImageView: CropImageView) {
    this.cropImageView = cropImageView
  }

  /**
   * Rotate the image in the crop image view.
   */
  open fun rotateImage(degrees: Int) {
    cropImageView?.rotateImage(degrees)
  }

  /**
   * Result with cropped image data or error if failed.
   */
  open fun setResult(uri: Uri?, error: Exception?, sampleSize: Int) {
    setResult(
      error?.let { CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE } ?: RESULT_OK,
      getResultIntent(uri, error, sampleSize),
    )
    finish()
  }

  /**
   * Cancel of cropping activity.
   */
  open fun setResultCancel() {
    setResult(RESULT_CANCELED)
    finish()
  }

  /**
   * Get intent instance to be used for the result of this activity.
   */
  open fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
    val result = CropImage.ActivityResult(
      originalUri = cropImageView?.imageUri,
      uriContent = uri,
      error = error,
      cropPoints = cropImageView?.cropPoints,
      cropRect = cropImageView?.cropRect,
      rotation = cropImageView?.rotatedDegrees ?: 0,
      wholeImageRect = cropImageView?.wholeImageRect,
      sampleSize = sampleSize,
    )
    val intent = Intent()
    intent.extras?.let(intent::putExtras)
    intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
    return intent
  }

  /**
   * Update the color of a specific menu item to the given color.
   */
  open fun updateMenuItemIconColor(menu: Menu, itemId: Int, color: Int) {
    val menuItem = menu.findItem(itemId)
    if (menuItem != null) {
      val menuItemIcon = menuItem.icon
      if (menuItemIcon != null) {
        try {
          menuItemIcon.apply {
            mutate()
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
              color,
              BlendModeCompat.SRC_ATOP,
            )
          }
          menuItem.icon = menuItemIcon
        } catch (e: Exception) {
          Log.w("AIC", "Failed to update menu item color", e)
        }
      }
    }
  }

  /**
   * Update the color of a specific menu item to the given color.
   */
  open fun updateMenuItemTextColor(menu: Menu, itemId: Int, color: Int) {
    val menuItem = menu.findItem(itemId) ?: return
    val menuTitle = menuItem.title
    if (menuTitle?.isNotBlank() == true) {
      try {
        val spannableTitle: Spannable = SpannableString(menuTitle)
        spannableTitle.setSpan(
          ForegroundColorSpan(color),
          0,
          spannableTitle.length,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        menuItem.title = spannableTitle
      } catch (e: Exception) {
        Log.w("AIC", "Failed to update menu item color", e)
      }
    }
  }

  enum class Source { CAMERA, GALLERY }

  private companion object {

    const val BUNDLE_KEY_TMP_URI = "bundle_key_tmp_uri"
  }
}
