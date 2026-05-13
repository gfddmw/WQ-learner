package com.example.wq_learner1.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneAuthGatewayTest {
    @Test
    fun aliyunFailureMessageShowsReadableCodeAndMessage() {
        val payload = """{"carrierFailedResultData":"","code":"600002","msg":"唤起授权页失败","requestId":"req-1"}"""

        assertEquals("号码认证失败：唤起授权页失败（600002）", payload.toAliyunFailureMessage())
    }
}
