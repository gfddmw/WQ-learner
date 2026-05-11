import json
import os
from typing import Any, Protocol, runtime_checkable

from .store import QuestionRecord


@runtime_checkable
class VariantGenerator(Protocol):
    def generate(self, source_question: QuestionRecord, topic: str) -> dict[str, str]:
        ...


class SimulatedVariantGenerator:
    def generate(self, source_question: QuestionRecord, topic: str) -> dict[str, str]:
        return {
            "source_question_id": source_question.id,
            "title": f"模拟变形题：{topic}",
            "content_md_latex": (
                f"基于原错题：{source_question.content_md_latex}\n\n"
                f"请围绕「{topic}」重新分析条件变化后的结论。"
            ),
            "answer_md_latex": "参考答案保持原知识点不变，请结合题目条件完成推导。",
            "explanation_md_latex": f"本题考查 {source_question.subject} / {source_question.chapter} 的同一核心方法。",
        }


class DashScopeVariantGenerator:
    def __init__(
        self,
        api_key: str,
        model: str = "qwen-plus",
        base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        client: Any | None = None,
    ) -> None:
        self.model = model
        self.client = client or create_openai_client(api_key=api_key, base_url=base_url)

    def generate(self, source_question: QuestionRecord, topic: str) -> dict[str, str]:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {
                    "role": "system",
                    "content": VARIANT_SYSTEM_PROMPT,
                },
                {
                    "role": "user",
                    "content": variant_user_prompt(source_question, topic),
                },
            ],
            temperature=0.3,
        )
        content = response.choices[0].message.content
        variant = parse_variant_json(content)
        variant["source_question_id"] = source_question.id
        return variant


VARIANT_SYSTEM_PROMPT = (
    "你是 11408 考研错题变式题生成助手。"
    "请基于用户给出的原错题生成一题同知识点、不同条件的变式题。"
    "只能返回 JSON，不要返回 Markdown 代码块。"
)


def variant_user_prompt(source_question: QuestionRecord, topic: str) -> str:
    return (
        "请根据这道错题生成变式题。\n"
        f"原错题 ID：{source_question.id}\n"
        f"科目：{source_question.subject}\n"
        f"章节：{source_question.chapter}\n"
        f"主题：{topic}\n"
        f"原题内容：{source_question.content_md_latex}\n\n"
        "返回 JSON 字段："
        "{\"title\":\"标题\","
        "\"content_md_latex\":\"Markdown + LaTeX 变式题题干\","
        "\"answer_md_latex\":\"Markdown + LaTeX 答案\","
        "\"explanation_md_latex\":\"Markdown + LaTeX 解析\"}"
    )


def create_openai_client(api_key: str, base_url: str) -> Any:
    try:
        from openai import OpenAI
    except ImportError as error:
        raise RuntimeError("缺少 openai 依赖，请先安装 backend/requirements.txt") from error
    return OpenAI(api_key=api_key, base_url=base_url)


def parse_variant_json(content: str) -> dict[str, str]:
    cleaned = content.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.strip("`")
        cleaned = cleaned.removeprefix("json").strip()
    data = json.loads(cleaned)
    return {
        "title": str(data["title"]),
        "content_md_latex": str(data["content_md_latex"]),
        "answer_md_latex": str(data["answer_md_latex"]),
        "explanation_md_latex": str(data["explanation_md_latex"]),
    }


def create_variant_generator() -> VariantGenerator:
    provider = os.environ.get("WQ_LEARNER_VARIANT_PROVIDER", "simulated").strip().lower()
    if provider == "dashscope":
        api_key = os.environ.get("DASHSCOPE_API_KEY", "")
        if not api_key:
            raise RuntimeError("缺少 DASHSCOPE_API_KEY 环境变量")
        return DashScopeVariantGenerator(
            api_key=api_key,
            model=os.environ.get("WQ_LEARNER_VARIANT_MODEL", "qwen-plus"),
            base_url=os.environ.get(
                "WQ_LEARNER_DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
            ),
        )
    return SimulatedVariantGenerator()


variant_service: VariantGenerator = create_variant_generator()
