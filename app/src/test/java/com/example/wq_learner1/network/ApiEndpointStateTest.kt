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
    fun localDevelopmentEndpointFallsBackToCloudEndpoint() {
        val state = ApiEndpointState()

        state.updateBaseUrl("http://10.0.2.2:8000")

        assertEquals(ApiConfig.FUNCTION_COMPUTE_BASE_URL, state.baseUrl)
        assertEquals("函数计算公网 API", state.environmentLabel)
    }

    @Test
    fun nonHttpsEndpointFallsBackToCloudEndpoint() {
        val state = ApiEndpointState()

        state.updateBaseUrl("http://wq-learner.example.com")

        assertEquals(ApiConfig.FUNCTION_COMPUTE_BASE_URL, state.baseUrl)
        assertEquals("函数计算公网 API", state.environmentLabel)
    }
}
