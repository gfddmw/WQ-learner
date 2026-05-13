from app.ocr import DashScopeVisionOcrService, SimulatedOcrService, create_ocr_service


def test_simulated_ocr_returns_question_text_and_classification_only():
    service = SimulatedOcrService()

    result = service.recognize(
        image_content=b"fake-image",
        content_type="image/png",
        image_url="oss://wq-learner/users/u/questions/q.png",
    )

    assert result.content_md_latex
    assert "$O(n)$" in result.content_md_latex
    assert result.confidence > 0
    assert result.answer_md_latex == ""
    assert result.explanation_md_latex == ""


def test_dashscope_ocr_sends_image_and_prompts_for_question_only():
    client = FakeOpenAiClient(
        content='{"content_md_latex":"TCP congestion window formula: $cwnd=2^k$.","subject":"Computer Networks","chapter":"Transport Layer","confidence":2}'
    )
    service = DashScopeVisionOcrService(
        api_key="sk-test",
        model="qwen3-vl-plus",
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        client=client,
    )

    result = service.recognize(
        image_content=b"jpeg-image",
        content_type="image/jpeg",
        image_url="oss://wq-learner/users/u/questions/q.jpg",
    )

    assert result.content_md_latex == "TCP congestion window formula: $cwnd=2^k$."
    assert result.subject == "Computer Networks"
    assert result.chapter == "Transport Layer"
    assert result.confidence == 2
    assert result.answer_md_latex == ""
    assert result.explanation_md_latex == ""
    assert client.calls[0]["model"] == "qwen3-vl-plus"
    user_content = client.calls[0]["messages"][1]["content"]
    system_prompt = client.calls[0]["messages"][0]["content"]
    prompt_text = user_content[0]["text"]
    assert "Do not generate an answer or explanation" in system_prompt
    assert "answer_md_latex" not in prompt_text
    assert "explanation_md_latex" not in prompt_text
    assert user_content[0]["type"] == "text"
    assert user_content[1]["type"] == "image_url"
    assert user_content[1]["image_url"]["url"].startswith("data:image/jpeg;base64,")


def test_create_ocr_service_uses_dashscope_when_configured(monkeypatch):
    monkeypatch.setenv("WQ_LEARNER_OCR_PROVIDER", "dashscope")
    monkeypatch.setenv("DASHSCOPE_API_KEY", "sk-test")
    monkeypatch.setenv("WQ_LEARNER_OCR_MODEL", "qwen3-vl-plus")
    monkeypatch.setenv("WQ_LEARNER_DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")

    service = create_ocr_service()

    assert isinstance(service, DashScopeVisionOcrService)


class FakeOpenAiClient:
    def __init__(self, content: str):
        self.calls = []
        self.chat = FakeChat(self, content)


class FakeChat:
    def __init__(self, owner: FakeOpenAiClient, content: str):
        self.completions = FakeCompletions(owner, content)


class FakeCompletions:
    def __init__(self, owner: FakeOpenAiClient, content: str):
        self.owner = owner
        self.content = content

    def create(self, **kwargs):
        self.owner.calls.append(kwargs)
        return FakeResponse(self.content)


class FakeResponse:
    def __init__(self, content: str):
        self.choices = [FakeChoice(content)]


class FakeChoice:
    def __init__(self, content: str):
        self.message = FakeMessage(content)


class FakeMessage:
    def __init__(self, content: str):
        self.content = content
