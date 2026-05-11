from email.parser import BytesParser
from email.policy import default

from fastapi import Depends, FastAPI, Header, HTTPException, Request

from .models import (
    AuthRequest,
    ConfirmQuestionRequest,
    DrawOriginalRequest,
    PracticeResponse,
    QuestionResponse,
    ReviewPracticeRequest,
    TokenResponse,
    UserProfile,
    VariantPracticeRequest,
)
from .image_storage import image_storage
from .ocr import ocr_service
from .store import QuestionRecord, UserRecord, store
from .variant_generator import variant_service

app = FastAPI(title="WQ Learner API")


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "ok",
        "service": "wq-learner-api",
        "runtime": "fastapi",
    }


def to_question_response(record: QuestionRecord) -> QuestionResponse:
    return QuestionResponse(
        id=record.id,
        user_id=record.user_id,
        image_url=record.image_url,
        content_md_latex=record.content_md_latex,
        subject=record.subject,
        chapter=record.chapter,
        status=record.status,
        mastery=record.mastery,
    )


def current_user(authorization: str = Header(default="")) -> UserRecord:
    prefix = "Bearer "
    if not authorization.startswith(prefix):
        raise HTTPException(status_code=401, detail="Missing bearer token")
    token = authorization.removeprefix(prefix)
    user = store.user_for_token(token)
    if user is None:
        raise HTTPException(status_code=401, detail="Invalid bearer token")
    return user


@app.post("/auth/register", response_model=UserProfile)
def register(request: AuthRequest) -> UserProfile:
    user = store.register(request.email, request.password)
    return UserProfile(id=user.id, email=user.email)


@app.post("/auth/login", response_model=TokenResponse)
def login(request: AuthRequest) -> TokenResponse:
    token = store.login(request.email, request.password)
    if token is None:
        raise HTTPException(status_code=401, detail="Invalid email or password")
    return TokenResponse(access_token=token)


@app.get("/me", response_model=UserProfile)
def me(user: UserRecord = Depends(current_user)) -> UserProfile:
    return UserProfile(id=user.id, email=user.email)


@app.post("/questions/upload", response_model=QuestionResponse)
async def upload_question(
    request: Request,
    user: UserRecord = Depends(current_user),
) -> QuestionResponse:
    content_type = request.headers.get("content-type", "")
    body = await request.body()
    image_content, image_content_type = extract_image_upload(content_type, body)
    accepts_image = (
        image_content_type in {"image/png", "image/jpeg", "image/jpg"}
    )
    if not accepts_image or not image_content:
        raise HTTPException(status_code=400, detail="Only PNG and JPEG uploads are supported")
    stored_image = image_storage.save_upload(
        user_id=user.id,
        content=image_content,
        content_type=image_content_type,
    )
    ocr_result = ocr_service.recognize(
        image_content=image_content,
        content_type=image_content_type,
        image_url=stored_image.image_url,
    )
    record = store.create_upload_draft(
        user_id=user.id,
        image_url=stored_image.image_url,
        content_md_latex=ocr_result.content_md_latex,
        subject=ocr_result.subject,
        chapter=ocr_result.chapter,
    )
    return to_question_response(record)


def extract_image_upload(content_type: str, body: bytes) -> tuple[bytes, str]:
    normalized = content_type.split(";")[0].strip().lower()
    if normalized in {"image/png", "image/jpeg", "image/jpg"}:
        return body, normalized
    if normalized != "multipart/form-data":
        return b"", normalized

    message = BytesParser(policy=default).parsebytes(
        b"Content-Type: " + content_type.encode("utf-8") + b"\r\n\r\n" + body
    )
    for part in message.iter_parts():
        part_type = part.get_content_type()
        if part_type in {"image/png", "image/jpeg", "image/jpg"}:
            return part.get_payload(decode=True) or b"", part_type
    return b"", normalized


@app.post("/questions/{question_id}/confirm", response_model=QuestionResponse)
def confirm_question(
    question_id: str,
    request: ConfirmQuestionRequest,
    user: UserRecord = Depends(current_user),
) -> QuestionResponse:
    record = store.confirm_question(
        question_id=question_id,
        user_id=user.id,
        content_md_latex=request.content_md_latex,
        subject=request.subject,
        chapter=request.chapter,
        mastery=request.mastery,
    )
    if record is None:
        raise HTTPException(status_code=404, detail="Question not found")
    return to_question_response(record)


@app.get("/questions", response_model=list[QuestionResponse])
def list_questions(
    subject: str | None = None,
    chapter: str | None = None,
    user: UserRecord = Depends(current_user),
) -> list[QuestionResponse]:
    return [
        to_question_response(record)
        for record in store.list_questions(user.id, subject, chapter)
    ]


@app.get("/questions/{question_id}", response_model=QuestionResponse)
def get_question(
    question_id: str,
    user: UserRecord = Depends(current_user),
) -> QuestionResponse:
    record = store.get_question(question_id, user.id)
    if record is None:
        raise HTTPException(status_code=404, detail="Question not found")
    return to_question_response(record)


@app.patch("/questions/{question_id}", response_model=QuestionResponse)
def update_question(
    question_id: str,
    request: ConfirmQuestionRequest,
    user: UserRecord = Depends(current_user),
) -> QuestionResponse:
    return confirm_question(question_id, request, user)


@app.post("/practice/original", response_model=PracticeResponse)
def draw_original(
    request: DrawOriginalRequest,
    user: UserRecord = Depends(current_user),
) -> PracticeResponse:
    practice = store.create_original_practice(
        user_id=user.id,
        count=request.count,
        subject=request.subject,
        chapter=request.chapter,
    )
    questions = [to_question_response(record) for record in store.questions_by_ids(user.id, practice.question_ids)]
    return PracticeResponse(id=practice.id, mode=practice.mode, questions=questions)


@app.post("/practice/variant", response_model=PracticeResponse)
def draw_variant(
    request: VariantPracticeRequest,
    user: UserRecord = Depends(current_user),
) -> PracticeResponse:
    source_question = store.get_question(request.source_question_id, user.id)
    if source_question is None:
        raise HTTPException(status_code=404, detail="Source question not found")
    variant = variant_service.generate(source_question, request.topic)
    practice = store.create_variant_practice(
        user_id=user.id,
        source_question_id=request.source_question_id,
        topic=request.topic,
        variant=variant,
    )
    return PracticeResponse(id=practice.id, mode=practice.mode, variant=practice.variant)


@app.post("/practice/{practice_id}/review", response_model=PracticeResponse)
def review_practice(
    practice_id: str,
    request: ReviewPracticeRequest,
    user: UserRecord = Depends(current_user),
) -> PracticeResponse:
    practice = store.review_practice(practice_id, user.id, request.result)
    if practice is None:
        raise HTTPException(status_code=404, detail="Practice session not found")
    questions = [to_question_response(record) for record in store.questions_by_ids(user.id, practice.question_ids)]
    return PracticeResponse(
        id=practice.id,
        mode=practice.mode,
        questions=questions,
        variant=practice.variant,
    )
