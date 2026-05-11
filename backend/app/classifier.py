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
