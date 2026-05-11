package com.example.wq_learner1.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiEndpointStateTest {
    @Test
    fun startsWithLocalDevelopmentEndpoint() {
        val state = ApiEndpointState()

        assertEquals(ApiConfig.LOCAL_DEVELOPMENT_BASE_URL, state.baseUrl)
        assertEquals("本地开发", state.environmentLabel)
        assertTrue(state.statusText.contains(ApiConfig.LOCAL_DEVELOPMENT_BASE_URL))
    }

    @Test
    fun switchesToCloudEndpointAndTrimsTrailingSlash() {
        val state = ApiEndpointState()

        state.updateBaseUrl(" https://wq-learner.example.com/ ")

        assertEquals("https://wq-learner.example.com", state.baseUrl)
        assertEquals("函数计算公网 API", state.environmentLabel)
    }

    @Test
    fun blankEndpointFallsBackToLocalDevelopmentEndpoint() {
        val state = ApiEndpointState(initialBaseUrl = "https://wq-learner.example.com")

        state.updateBaseUrl("   ")

        assertEquals(ApiConfig.LOCAL_DEVELOPMENT_BASE_URL, state.baseUrl)
        assertEquals("本地开发", state.environmentLabel)
    }
}
