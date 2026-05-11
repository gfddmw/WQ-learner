package com.example.wq_learner1.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiEndpointStateTest {
    @Test
    fun startsWithCloudEndpointByDefault() {
        val state = ApiEndpointState()

        assertEquals(ApiConfig.FUNCTION_COMPUTE_BASE_URL, state.baseUrl)
        assertEquals("函数计算公网 API", state.environmentLabel)
        assertTrue(state.statusText.contains(ApiConfig.FUNCTION_COMPUTE_BASE_URL))
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

        assertEquals(ApiConfig.FUNCTION_COMPUTE_BASE_URL, state.baseUrl)
        assertEquals("函数计算公网 API", state.environmentLabel)
    }

    @Test
    fun localDevelopmentEndpointCanStillBeSelected() {
        val state = ApiEndpointState()

        state.updateBaseUrl(ApiConfig.LOCAL_DEVELOPMENT_BASE_URL)

        assertEquals(ApiConfig.LOCAL_DEVELOPMENT_BASE_URL, state.baseUrl)
        assertEquals("本地开发", state.environmentLabel)
    }
}
