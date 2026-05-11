package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCaptureStateTest {
    @Test
    fun startsWithoutPendingCapture() {
        val state = CameraCaptureState()

        assertFalse(state.hasPendingCapture)
        assertNull(state.pendingImageUri)
    }

    @Test
    fun prepareStoresPendingCameraUri() {
        val state = CameraCaptureState()
            .prepare("content://com.example.wq_learner1/cache/question.jpg")

        assertTrue(state.hasPendingCapture)
        assertEquals("content://com.example.wq_learner1/cache/question.jpg", state.pendingImageUri)
    }

    @Test
    fun successfulCaptureSelectsPendingImageAndClearsPendingUri() {
        val result = CameraCaptureState()
            .prepare("content://com.example.wq_learner1/cache/question.jpg")
            .complete(success = true)

        assertFalse(result.cameraState.hasPendingCapture)
        assertEquals("content://com.example.wq_learner1/cache/question.jpg", result.imageState?.selectedImageUri)
    }

    @Test
    fun cancelledCaptureClearsPendingUriWithoutSelectingImage() {
        val result = CameraCaptureState()
            .prepare("content://com.example.wq_learner1/cache/question.jpg")
            .complete(success = false)

        assertFalse(result.cameraState.hasPendingCapture)
        assertNull(result.imageState)
    }
}
