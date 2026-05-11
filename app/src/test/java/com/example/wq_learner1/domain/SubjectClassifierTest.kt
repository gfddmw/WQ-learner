package com.example.wq_learner1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectClassifierTest {
    @Test
    fun classifiesNetworkTcpQuestion() {
        val result = SubjectClassifier.classify("TCP 拥塞控制和滑动窗口")

        assertEquals("计算机网络", result.subject)
        assertEquals("传输层", result.chapter)
        assertTrue(result.confidence > 0)
    }

    @Test
    fun fallsBackWhenNoKeywordMatches() {
        val result = SubjectClassifier.classify("暂时无法判断的错题")

        assertEquals("待分类", result.subject)
        assertEquals("待选择", result.chapter)
        assertEquals(0, result.confidence)
    }
}
