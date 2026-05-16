import pytest
from app.classifier import classify_question, KEYWORD_MAP


@pytest.mark.parametrize(
    "text, expected_subject, expected_chapter",
    [
        # Math cases
        pytest.param("求极限 lim x->0 sin(x)/x", "高等数学", "极限与连续", id="math_limit"),
        pytest.param("已知矩阵 A 是一个 3x3 的奇异矩阵，求其秩", "线性代数", "矩阵", id="math_matrix"),
        pytest.param("设随机变量 X 服从正态分布，求数学期望", "概率统计", "随机变量及其分布", id="math_prob_expectation"),
        pytest.param("判断该向量组是否线性相关", "线性代数", "向量", id="math_vector_linear_dependence"),
        pytest.param("求多维随机变量的边缘分布", "概率统计", "多维随机变量", id="math_multivariate_dist"),
        # CS cases
        pytest.param("数据结构中，二叉树的遍历方式有哪些？", "数据结构", "树与二叉树", id="cs_ds_tree"),
        pytest.param("操作系统中的页面置换算法有哪些？", "操作系统", "内存管理", id="cs_os_memory"),
        pytest.param("TCP协议的三次握手过程", "计算机网络", "传输层", id="cs_network_transport"),
        pytest.param("HTTP状态码404表示什么", "计算机网络", "应用层", id="cs_network_app"),
        pytest.param("CPU流水线的工作原理", "计算机组成原理", "CPU", id="cs_arch_cpu"),
        # Robustness cases (Mixed case / Upper case)
        pytest.param("TCP协议的三次握手", "计算机网络", "传输层", id="robustness_tcp_upper"),
        pytest.param("tcp协议的三次握手", "计算机网络", "传输层", id="robustness_tcp_lower"),
        pytest.param("请简述CPU的工作原理", "计算机组成原理", "CPU", id="robustness_cpu_upper"),
        pytest.param("请简述cpu的工作原理", "计算机组成原理", "CPU", id="robustness_cpu_lower"),
        pytest.param("ALU的功能是什么", "计算机组成原理", "指令系统", id="robustness_alu_upper"),
        # Edge cases (Empty / Whitespace)
        pytest.param("   ", "待分类", "待选择", id="edge_whitespace"),
        pytest.param("", "待分类", "待选择", id="edge_empty"),
        pytest.param("这是一道无法判断来源的题目", "待分类", "待选择", id="edge_unknown"),
        # Tie-breaking test: First-Match Wins
        # "树" is in "数据结构" (1st in KEYWORD_MAP)
        # "进程" is in "操作系统" (3rd in KEYWORD_MAP)
        # "树 进程" should match "数据结构"
        pytest.param("树 进程", "数据结构", "树与二叉树", id="tie_break_first_match"),
    ],
)
def test_classify_question_cases(text, expected_subject, expected_chapter):
    """Test classification with various subjects, chapters and robustness cases."""
    result = classify_question(text)
    assert result.subject == expected_subject
    assert result.chapter == expected_chapter


def test_confidence_level():
    """Verify that confidence (score) is calculated correctly."""
    # "树" matches 1 keyword: "树"
    res_single = classify_question("树")
    # "树遍历" matches 2 keywords: "树", "遍历"
    res_multiple = classify_question("树遍历")

    assert res_single.confidence == 1
    assert res_multiple.confidence == 2
    assert res_multiple.subject == "数据结构"
    assert res_multiple.chapter == "树与二叉树"


def test_keyword_overlap():
    """Verify that '二叉树' matches both '树' and '二叉树', giving confidence 2."""
    # In KEYWORD_MAP["数据结构"]["树与二叉树"], we have ["树", "二叉树", ...]
    res = classify_question("二叉树")
    assert res.confidence == 2
    assert res.subject == "数据结构"


def test_low_confidence_for_unknown_content():
    """Verify unknown content results in 0 confidence and correct default values."""
    result = classify_question("这是一道无法判断来源的题目")
    assert result.confidence == 0
    assert result.subject == "待分类"
    assert result.chapter == "待选择"


def test_keyword_integrity():
    """Check for duplicate keywords across different subjects (Senior Quality)."""
    kw_to_subjects = {}
    duplicates = []

    for subject, chapters in KEYWORD_MAP.items():
        for chapter, keywords in chapters.items():
            for kw in keywords:
                kw_lower = kw.lower()
                if kw_lower in kw_to_subjects and kw_to_subjects[kw_lower] != subject:
                    duplicates.append((kw_lower, kw_to_subjects[kw_lower], subject))
                kw_to_subjects[kw_lower] = subject

    # Known acceptable overlaps across subjects
    # "向量" appears in both "高等数学" (Spatial Geometry) and "线性代数" (Linear Algebra)
    known_overlaps = {("向量", "高等数学", "线性代数")}
    
    actual_duplicates = set(duplicates)
    
    # Check if there are any unexpected duplicates
    unexpected = actual_duplicates - known_overlaps
    assert not unexpected, f"Unexpected duplicate keywords across subjects: {unexpected}"
    
    # Ensure known overlaps still exist (to verify the test logic itself)
    for overlap in known_overlaps:
        assert overlap in actual_duplicates, f"Expected overlap {overlap} not found"
