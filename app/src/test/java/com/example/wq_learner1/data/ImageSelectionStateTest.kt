package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSelectionStateTest {
    @Test
    fun startsEmptyWithPlaceholderPreview() {
        val state = ImageSelectionState()

        assertFalse(state.hasImage)
        assertEquals(null, state.selectedImageUri)
        assertEquals("尚未选择图片", state.previewLabel)
    }

    @Test
    fun selectImageStoresUriAndShowsFileName() {
        val state = ImageSelectionState()
            .select("content://media/external/images/media/wq-question.png")

        assertTrue(state.hasImage)
        assertEquals("content://media/external/images/media/wq-question.png", state.selectedImageUri)
        assertEquals("已选择：wq-question.png", state.previewLabel)
    }

    @Test
    fun clearImageReturnsToEmptyState() {
        val state = ImageSelectionState()
            .select("content://media/external/images/media/wq-question.png")
            .clear()

        assertFalse(state.hasImage)
        assertEquals(null, state.selectedImageUri)
        assertEquals("尚未选择图片", state.previewLabel)
    }
}
