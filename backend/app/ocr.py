import base64
import json
import os
from dataclasses import dataclass
from typing import Any, Protocol, runtime_checkable

from .classifier import classify_question


@dataclass(frozen=True)
class OcrResult:
    content_md_latex: str
    subject: str
    chapter: str
    confidence: int
    answer_md_latex: str = ""
    explanation_md_latex: str = ""


@runtime_checkable
class OcrService(Protocol):
    def recognize(self, image_content: bytes, content_type: str, image_url: str) -> OcrResult:
        ...


class SimulatedOcrService:
    def recognize(self, image_content: bytes, content_type: str, image_url: str) -> OcrResult:
        recognized = (
            "二叉树遍历与哈希查找综合题。请分析遍历过程，"
            "并写出平均时间复杂度 $O(n)$。"
        )
        classification = classify_question(recognized)
        return OcrResult(
            content_md_latex=recognized,
            subject=classification.subject,
            chapter=classification.chapter,
            confidence=classification.confidence,
            answer_md_latex="平均时间复杂度为 $O(n)$。",
            explanation_md_latex="二叉树遍历的时间复杂度通常与节点数成正比。",
        )


class DashScopeVisionOcrService:
    def __init__(
        self,
        api_key: str,
        model: str = "qwen3-vl-plus",
        base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        client: Any | None = None,
    ) -> None:
        self.api_key = api_key
        self.model = model
        self.base_url = base_url
        self.client = client or create_openai_client(api_key=api_key, base_url=base_url)

    def recognize(self, image_content: bytes, content_type: str, image_url: str) -> OcrResult:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {
                    "role": "system",
                    "content": OCR_SYSTEM_PROMPT,
                },
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": OCR_USER_PROMPT,
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": image_data_url(image_content, content_type),
                            },
                        },
                    ],
                },
            ],
            temperature=0,
        )
        content = response.choices[0].message.content
        return parse_ocr_json(content)


OCR_SYSTEM_PROMPT = (
    "你是 11408 考研错题 OCR 与公式结构化助手。"
    "请从图片中识别题干、公式、选项，并生成对应的答案和详细解析，输出 Markdown + LaTeX。"
    "只能返回 JSON，不要返回 Markdown 代码块。"
)

OCR_USER_PROMPT = (
    "请识别这张错题图片，返回 JSON："
    "{\"content_md_latex\":\"Markdown + LaTeX 题干\","
    "\"subject\":\"数据结构/计算机组成原理/操作系统/计算机网络/待分类\","
    "\"chapter\":\"章节\","
    "\"confidence\":1,"
    "\"answer_md_latex\":\"Markdown + LaTeX 答案\","
    "\"explanation_md_latex\":\"Markdown + LaTeX 解析\"}"
)


def create_openai_client(api_key: str, base_url: str) -> Any:
    try:
        from openai import OpenAI
    except ImportError as error:
        raise RuntimeError("缺少 openai 依赖，请先安装 backend/requirements.txt") from error
    return OpenAI(api_key=api_key, base_url=base_url)


def image_data_url(image_content: bytes, content_type: str) -> str:
    encoded = base64.b64encode(image_content).decode("ascii")
    return f"data:{content_type};base64,{encoded}"


def parse_ocr_json(content: str) -> OcrResult:
    cleaned = content.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.strip("`")
        cleaned = cleaned.removeprefix("json").strip()
    data = json.loads(cleaned)
    return OcrResult(
        content_md_latex=str(data["content_md_latex"]),
        subject=str(data["subject"]),
        chapter=str(data["chapter"]),
        confidence=int(data.get("confidence", 1)),
        answer_md_latex=str(data.get("answer_md_latex", "")),
        explanation_md_latex=str(data.get("explanation_md_latex", "")),
    )


def create_ocr_service() -> OcrService:
    provider = os.environ.get("WQ_LEARNER_OCR_PROVIDER", "simulated").strip().lower()
    if provider == "dashscope":
        api_key = os.environ.get("DASHSCOPE_API_KEY", "")
        if not api_key:
            raise RuntimeError("缺少 DASHSCOPE_API_KEY 环境变量")
        return DashScopeVisionOcrService(
            api_key=api_key,
            model=os.environ.get("WQ_LEARNER_OCR_MODEL", "qwen3-vl-plus"),
            base_url=os.environ.get(
                "WQ_LEARNER_DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
            ),
        )
    return SimulatedOcrService()


ocr_service: OcrService = create_ocr_service()
