package com.example.wq_learner1.data

data class ImageSelectionState(
    val selectedImageUri: String? = null,
) {
    val hasImage: Boolean
        get() = selectedImageUri != null

    val previewLabel: String
        get() = selectedImageUri?.let { "已选择：${it.substringAfterLast('/')}" } ?: "尚未选择图片"

    fun select(uri: String): ImageSelectionState {
        return copy(selectedImageUri = uri)
    }

    fun clear(): ImageSelectionState {
        return copy(selectedImageUri = null)
    }
}
