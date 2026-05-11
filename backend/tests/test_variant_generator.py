from app.store import QuestionRecord
from app.variant_generator import (
    DashScopeVariantGenerator,
    SimulatedVariantGenerator,
    create_variant_generator,
)


def test_simulated_variant_generator_uses_source_question_context():
    source = QuestionRecord(
        id="Q-001",
        user_id="U-1",
        image_url="oss://wq-learner/q.jpg",
        content_md_latex="二叉树遍历的时间复杂度是 $O(n)$。",
        subject="数据结构",
        chapter="树与二叉树",
    )
    generator = SimulatedVariantGenerator()

    variant = generator.generate(source, topic="树与二叉树")

    assert variant["source_question_id"] == "Q-001"
    assert "树与二叉树" in variant["title"]
    assert "$O(n)$" in variant["content_md_latex"]
    assert variant["answer_md_latex"]
    assert variant["explanation_md_latex"]


def test_dashscope_variant_generator_sends_source_question_and_parses_json():
    source = QuestionRecord(
        id="Q-002",
        user_id="U-1",
        image_url="oss://wq-learner/q.jpg",
        content_md_latex="TCP 慢开始中拥塞窗口按指数增长。",
        subject="计算机网络",
        chapter="传输层",
    )
    client = FakeOpenAiClient(
        content=(
            '{"title":"TCP 拥塞控制变式题",'
            '"content_md_latex":"若 $ssthresh=16$，求第 5 轮窗口。",'
            '"answer_md_latex":"$cwnd=16$",'
            '"explanation_md_latex":"慢开始阶段窗口指数增长。"}'
        )
    )
    generator = DashScopeVariantGenerator(
        api_key="sk-test",
        model="qwen-plus",
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        client=client,
    )

    variant = generator.generate(source, topic="传输层")

    assert variant["source_question_id"] == "Q-002"
    assert variant["title"] == "TCP 拥塞控制变式题"
    assert variant["answer_md_latex"] == "$cwnd=16$"
    assert client.calls[0]["model"] == "qwen-plus"
    messages = client.calls[0]["messages"]
    assert "TCP 慢开始" in messages[1]["content"]
    assert "传输层" in messages[1]["content"]


def test_create_variant_generator_uses_dashscope_when_configured(monkeypatch):
    monkeypatch.setenv("WQ_LEARNER_VARIANT_PROVIDER", "dashscope")
    monkeypatch.setenv("DASHSCOPE_API_KEY", "sk-test")
    monkeypatch.setenv("WQ_LEARNER_VARIANT_MODEL", "qwen-plus")
    monkeypatch.setenv("WQ_LEARNER_DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")

    generator = create_variant_generator()

    assert isinstance(generator, DashScopeVariantGenerator)


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
