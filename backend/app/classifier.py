from dataclasses import dataclass


@dataclass(frozen=True)
class ClassificationResult:
    subject: str
    chapter: str
    confidence: int


KEYWORD_MAP: dict[str, dict[str, list[str]]] = {
    "数据结构": {
        "树与二叉树": ["树", "二叉树", "遍历", "b树", "b-tree"],
        "查找与散列": ["哈希", "散列", "查找", "hash"],
        "图": ["图", "最短路径", "拓扑", "dfs", "bfs"],
        "排序": ["排序", "快排", "归并", "堆排序"],
    },
    "计算机组成原理": {
        "存储系统": ["cache", "缓存", "主存", "存储", "页表"],
        "指令系统": ["指令", "寻址", "alu", "操作数"],
        "CPU": ["流水线", "周期", "中断", "控制器"],
    },
    "操作系统": {
        "进程管理": ["进程", "线程", "调度", "信号量", "死锁"],
        "内存管理": ["虚拟内存", "页面", "缺页", "置换"],
        "文件系统": ["文件", "目录", "磁盘"],
    },
    "计算机网络": {
        "传输层": ["tcp", "udp", "拥塞", "滑动窗口", "可靠传输"],
        "网络层": ["ip", "路由", "子网", "icmp"],
        "应用层": ["http", "dns", "smtp"],
    },
    "高等数学": {
        "极限与连续": ["极限", "连续", "间断点", "无穷小", "泰勒"],
        "一元函数微积分": ["导数", "微分", "不定积分", "定积分", "中值定理", "凹凸性", "极值"],
        "多元函数微积分": ["偏导数", "全微分", "重积分", "雅可比", "散度", "旋度"],
        "无穷级数": ["级数", "收敛", "幂级数", "傅里叶"],
        "常微分方程": ["微分方程", "通解", "特解", "齐次"],
        "空间解析几何": ["向量", "直线", "平面", "曲面", "二次曲面"],
    },
    "线性代数": {
        "行列式": ["行列式", "逆序数", "余子式"],
        "矩阵": ["矩阵", "逆矩阵", "伴随", "秩", "初等变换"],
        "向量": ["向量组", "线性相关", "线性无关", "极大无关组"],
        "线性方程组": ["线性方程组", "克拉默", "基础解系"],
        "特征值与特征向量": ["特征值", "特征向量", "相似", "对角化"],
        "二次型": ["二次型", "合同", "正定"],
    },
    "概率统计": {
        "随机事件与概率": ["概率", "随机事件", "贝叶斯", "条件概率"],
        "随机变量及其分布": ["随机变量", "分布函数", "密度函数", "泊松", "正态分布"],
        "多维随机变量": ["二维随机变量", "边缘分布", "独立性", "协方差"],
        "数字特征": ["期望", "数学期望", "方差", "矩", "相关系数"],
        "数理统计基础": ["样本", "统计量", "卡方分布", "t分布", "f分布"],
        "参数估计与假设检验": ["参数估计", "极大似然", "置信区间", "假设检验"],
    },
}


def classify_question(text: str) -> ClassificationResult:
    normalized = text.lower()
    best_subject = "待分类"
    best_chapter = "待选择"
    best_score = 0

    for subject, chapters in KEYWORD_MAP.items():
        for chapter, keywords in chapters.items():
            score = sum(1 for keyword in keywords if keyword.lower() in normalized)
            if score > best_score:
                best_subject = subject
                best_chapter = chapter
                best_score = score

    return ClassificationResult(
        subject=best_subject,
        chapter=best_chapter,
        confidence=best_score,
    )
