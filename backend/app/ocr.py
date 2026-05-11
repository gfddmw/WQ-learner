from dataclasses import dataclass
from typing import Protocol, runtime_checkable

from .classifier import classify_question


@dataclass(frozen=True)
class OcrResult:
    content_md_latex: str
    subject: str
    chapter: str
    confidence: int


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
        )


def create_ocr_service() -> OcrService:
    return SimulatedOcrService()


ocr_service: OcrService = create_ocr_service()
