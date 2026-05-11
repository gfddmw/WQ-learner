from app.ocr import DashScopeVisionOcrService, SimulatedOcrService, create_ocr_service


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


def test_dashscope_ocr_sends_image_and_parses_json_result():
    client = FakeOpenAiClient(
        content='{"content_md_latex":"TCP 拥塞控制公式：$cwnd=2^k$。","subject":"计算机网络","chapter":"传输层","confidence":2}'
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

    assert result.content_md_latex == "TCP 拥塞控制公式：$cwnd=2^k$。"
    assert result.subject == "计算机网络"
    assert result.chapter == "传输层"
    assert result.confidence == 2
    assert client.calls[0]["model"] == "qwen3-vl-plus"
    user_content = client.calls[0]["messages"][1]["content"]
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
