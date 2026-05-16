from app.classifier import classify_question


def test_classifies_data_structure_tree_question():
    result = classify_question("二叉树遍历和哈希查找的平均时间复杂度是多少？")

    assert result.subject == "数据结构"
    assert result.chapter == "树与二叉树"
    assert result.confidence > 0


def test_low_confidence_for_unknown_content():
    result = classify_question("这是一道无法判断来源的题目")

    assert result.subject == "待分类"
    assert result.chapter == "待选择"
    assert result.confidence == 0


def test_classifies_new_keywords():
    # Test CPU
    result = classify_question("请简述CPU流水线的工作原理")
    assert result.subject == "计算机组成原理"
    assert result.chapter == "CPU"

    # Test 内存
    result = classify_question("虚拟内存的置换算法有哪些")
    assert result.subject == "操作系统"
    assert result.chapter == "内存管理"

    # Test 向量
    result = classify_question("判断该向量组是否线性相关")
    assert result.subject == "线性代数"
    assert result.chapter == "向量"

    # Test 多维
    result = classify_question("求多维随机变量的边缘分布")
    assert result.subject == "概率统计"
    assert result.chapter == "多维随机变量"


def test_classify_math():
    from app.classifier import classify_question
    
    res1 = classify_question("求极限 lim x->0 sin(x)/x")
    assert res1.subject == "高等数学"
    assert res1.chapter == "极限与连续"

    res2 = classify_question("已知矩阵 A 是一个 3x3 的奇异矩阵，求其秩")
    assert res2.subject == "线性代数"
    assert res2.chapter == "矩阵"

    res3 = classify_question("设随机变量 X 服从正态分布，求数学期望")
    assert res3.subject == "概率统计"
    assert res3.chapter == "数字特征"
