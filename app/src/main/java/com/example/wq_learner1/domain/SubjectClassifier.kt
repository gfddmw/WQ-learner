package com.example.wq_learner1.domain

data class ClassificationResult(
    val subject: String,
    val chapter: String,
    val confidence: Int,
)

object SubjectClassifier {
    private val keywordMap: Map<String, Map<String, List<String>>> = mapOf(
        "数据结构" to mapOf(
            "树与二叉树" to listOf("数据结构", "树", "二叉树", "遍历", "b树", "b-tree"),
            "查找与散列" to listOf("哈希", "散列", "查找", "hash"),
            "图" to listOf("图", "最短路径", "拓扑", "dfs", "bfs"),
            "排序" to listOf("排序", "快排", "归并", "堆排序"),
        ),
        "计算机组成原理" to mapOf(
            "存储系统" to listOf("计算机组成原理", "cache", "缓存", "主存", "存储", "页表"),
            "指令系统" to listOf("指令", "寻址", "alu", "操作数"),
            "CPU" to listOf("流水线", "周期", "中断", "控制器", "cpu"),
        ),
        "操作系统" to mapOf(
            "进程管理" to listOf("操作系统", "进程", "线程", "调度", "信号量", "死锁"),
            "内存管理" to listOf("虚拟内存", "页面", "缺页", "置换", "内存"),
            "文件系统" to listOf("文件", "目录", "磁盘"),
        ),
        "计算机网络" to mapOf(
            "传输层" to listOf("计算机网络", "tcp", "udp", "拥塞", "滑动窗口", "可靠传输"),
            "网络层" to listOf("ip", "路由", "子网", "icmp"),
            "应用层" to listOf("http", "dns", "smtp"),
        ),
        "高等数学" to mapOf(
            "极限与连续" to listOf("高等数学", "极限", "连续", "间断点", "无穷小", "泰勒"),
            "一元函数微积分" to listOf("导数", "微分", "不定积分", "定积分", "中值定理", "凹凸性", "极值"),
            "多元函数微积分" to listOf("偏导数", "全微分", "重积分", "雅可比", "散度", "旋度"),
            "无穷级数" to listOf("级数", "收敛", "幂级数", "傅里叶"),
            "常微分方程" to listOf("微分方程", "通解", "特解", "齐次"),
            "空间解析几何" to listOf("向量", "直线", "平面", "曲面", "二次曲面"),
        ),
        "线性代数" to mapOf(
            "行列式" to listOf("线性代数", "行列式", "逆序数", "余子式"),
            "矩阵" to listOf("矩阵", "逆矩阵", "伴随", "秩", "初等变换"),
            "向量" to listOf("向量组", "线性相关", "线性无关", "极大无关组", "向量"),
            "线性方程组" to listOf("线性方程组", "克拉默", "基础解系"),
            "特征值与特征向量" to listOf("特征值", "特征向量", "相似", "对角化"),
            "二次型" to listOf("二次型", "合同", "正定"),
        ),
        "概率统计" to mapOf(
            "随机事件与概率" to listOf("概率统计", "概率", "随机事件", "贝叶斯", "条件概率"),
            "随机变量及其分布" to listOf("随机变量", "分布函数", "密度函数", "泊松", "正态分布"),
            "多维随机变量" to listOf("二维随机变量", "边缘分布", "独立性", "协方差", "多维"),
            "数字特征" to listOf("期望", "数学期望", "方差", "矩", "相关系数"),
            "数理统计基础" to listOf("样本", "统计量", "卡方分布", "t分布", "f分布"),
            "参数估计与假设检验" to listOf("参数估计", "极大似然", "置信区间", "假设检验"),
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
