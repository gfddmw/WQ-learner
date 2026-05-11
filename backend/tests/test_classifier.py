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
