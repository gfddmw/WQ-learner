from pydantic import BaseModel, Field


class AuthRequest(BaseModel):
    email: str
    password: str = Field(min_length=6)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserProfile(BaseModel):
    id: str
    email: str


class QuestionResponse(BaseModel):
    id: str
    user_id: str
    image_url: str
    content_md_latex: str
    subject: str
    chapter: str
    status: str
    mastery: str
    answer_md_latex: str = ""
    explanation_md_latex: str = ""


class ConfirmQuestionRequest(BaseModel):
    content_md_latex: str
    subject: str
    chapter: str
    mastery: str = "reviewing"
    answer_md_latex: str = ""
    explanation_md_latex: str = ""


class DrawOriginalRequest(BaseModel):
    count: int = Field(default=1, ge=1, le=20)
    subject: str | None = None
    chapter: str | None = None


class PracticeResponse(BaseModel):
    id: str
    mode: str
    questions: list[QuestionResponse] = []
    variant: dict | None = None


class VariantPracticeRequest(BaseModel):
    source_question_id: str
    topic: str


class ReviewPracticeRequest(BaseModel):
    result: str
