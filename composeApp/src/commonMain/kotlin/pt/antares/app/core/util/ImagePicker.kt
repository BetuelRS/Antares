package pt.antares.app.core.util

import androidx.compose.runtime.Composable

data class PickedImage(val base64: String, val mime: String)

const val MAX_IMAGE_DIMEN = 1024
const val MAX_IMAGE_BYTES = 1_500_000

const val MAX_LABEL_DIMEN = 2048

expect class ImagePickerController {
    fun takePhoto()
    fun pickFromGallery()
}

@Composable
expect fun rememberImagePicker(
    maxDimen: Int = MAX_IMAGE_DIMEN,
    onImage: (PickedImage) -> Unit,
): ImagePickerController
