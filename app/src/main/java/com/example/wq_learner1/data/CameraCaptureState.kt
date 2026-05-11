package com.example.wq_learner1.data

data class CameraCaptureState(
    val pendingImageUri: String? = null,
) {
    val hasPendingCapture: Boolean
        get() = pendingImageUri != null

    fun prepare(uri: String): CameraCaptureState {
        return copy(pendingImageUri = uri)
    }

    fun complete(success: Boolean): CaptureResult {
        return if (success && pendingImageUri != null) {
            CaptureResult(
                cameraState = copy(pendingImageUri = null),
                imageState = ImageSelectionState().select(pendingImageUri),
            )
        } else {
            CaptureResult(
                cameraState = copy(pendingImageUri = null),
                imageState = null,
            )
        }
    }
}

data class CaptureResult(
    val cameraState: CameraCaptureState,
    val imageState: ImageSelectionState?,
)
