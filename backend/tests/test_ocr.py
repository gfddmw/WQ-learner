from app.ocr import SimulatedOcrService


def test_simulated_ocr_returns_markdown_latex_and_classification():
    service = SimulatedOcrService()

    result = service.recognize(
        image_content=b"fake-image",
        content_type="image/png",
        image_url="oss://wq-learner/users/u/questions/q.png",
    )

    assert result.content_md_latex
    assert "$O(n)$" in result.content_md_latex
    assert result.subject == "数据结构"
    assert result.chapter == "树与二叉树"
    assert result.confidence > 0
