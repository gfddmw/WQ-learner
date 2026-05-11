package com.example.wq_learner1.domain

data class ClassificationResult(
    val subject: String,
    val chapter: String,
    val confidence: Int,
)

object SubjectClassifier {
    private val keywordMap: Map<String, Map<String, List<String>>> = mapOf(
        "数据结构" to mapOf(
            "树与二叉树" to listOf("树", "二叉树", "遍历", "b树", "b-tree"),
            "查找与散列" to listOf("哈希", "散列", "查找", "hash"),
            "图" to listOf("图", "最短路径", "拓扑", "dfs", "bfs"),
            "排序" to listOf("排序", "快排", "归并", "堆排序"),
        ),
        "计算机组成原理" to mapOf(
            "存储系统" to listOf("cache", "缓存", "主存", "存储", "页表"),
            "指令系统" to listOf("指令", "寻址", "alu", "操作数"),
            "CPU" to listOf("流水线", "周期", "中断", "控制器"),
        ),
        "操作系统" to mapOf(
            "进程管理" to listOf("进程", "线程", "调度", "信号量", "死锁"),
            "内存管理" to listOf("虚拟内存", "页面", "缺页", "置换"),
            "文件系统" to listOf("文件", "目录", "磁盘"),
        ),
        "计算机网络" to mapOf(
            "传输层" to listOf("tcp", "udp", "拥塞", "滑动窗口", "可靠传输"),
            "网络层" to listOf("ip", "路由", "子网", "icmp"),
            "应用层" to listOf("http", "dns", "smtp"),
        ),
    )

    fun classify(text: String): ClassificationResult {
        val normalized = text.lowercase()
        var bestSubject = "待分类"
        var bestChapter = "待选择"
        var bestScore = 0

        keywordMap.forEach { (subject, chapters) ->
            chapters.forEach { (chapter, keywords) ->
                val score = keywords.count { keyword -> normalized.contains(keyword.lowercase()) }
                if (score > bestScore) {
                    bestSubject = subject
                    bestChapter = chapter
                    bestScore = score
                }
            }
        }

        return ClassificationResult(bestSubject, bestChapter, bestScore)
    }
}
