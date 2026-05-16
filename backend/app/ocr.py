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
        recognized = "Analyze binary tree traversal and hash lookup. Give the average time complexity $O(n)$."
        classification = classify_question(recognized)
        return OcrResult(
            content_md_latex=recognized,
            subject=classification.subject,
            chapter=classification.chapter,
            confidence=classification.confidence,
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
    "You are an OCR and classification assistant for 11408 exam mistake questions. "
    "Only recognize the question text, formulas, options, subject, and chapter from the image. "
    "Do not generate an answer or explanation. Return JSON only, without Markdown fences."
)

OCR_USER_PROMPT = (
    "Recognize this mistake-question image and return JSON with exactly these fields: "
    '{"content_md_latex":"question stem/options in Markdown + LaTeX",'
    '"subject":"数据结构/计算机组成原理/操作系统/计算机网络/高等数学/线性代数/概率统计/待分类",'
    '"chapter":"chapter",'
    '"confidence":1}'
)


def create_openai_client(api_key: str, base_url: str) -> Any:
    try:
        from openai import OpenAI
    except ImportError as error:
        raise RuntimeError("missing openai dependency; install backend/requirements.txt") from error
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
            raise RuntimeError("missing DASHSCOPE_API_KEY environment variable")
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
