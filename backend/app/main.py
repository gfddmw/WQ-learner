from email.parser import BytesParser
from email.policy import default
import os

from fastapi import Depends, FastAPI, Header, HTTPException, Request

from .models import (
    AuthRequest,
    ConfirmQuestionRequest,
    DrawOriginalRequest,
    OneClickPhoneLoginRequest,
    PracticeResponse,
    QuestionResponse,
    ReviewPracticeRequest,
    SmsCodeRequest,
    SmsCodeResponse,
    SmsLoginRequest,
    TokenResponse,
    UserProfile,
    VariantPracticeRequest,
)
from .image_storage import image_storage
from .ocr import ocr_service
from .phone_auth import PhoneAuthConfigurationError, PhoneAuthError, phone_auth_service
from .sms import SmsConfigurationError, SmsSendError, normalize_phone, sms_code_store, sms_service
from .store import TABLE_USERS, QuestionRecord, UserRecord, store
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
        answer_md_latex=record.answer_md_latex,
        explanation_md_latex=record.explanation_md_latex,
    )


def mask_config_value(value: str) -> str:
    if not value:
        return ""
    if len(value) <= 8:
        return f"{value[:1]}***{value[-1:]}({len(value)})"
    return f"{value[:4]}***{value[-4:]}({len(value)})"


def current_user(authorization: str = Header(default="")) -> UserRecord:
    prefix = "Bearer "
    if not authorization.startswith(prefix):
        raise HTTPException(status_code=401, detail="Missing bearer token")
    token = authorization.removeprefix(prefix)
    user = store.user_for_token(token)
    if user is None:
        raise HTTPException(status_code=401, detail="Invalid bearer token")
    return user


@app.get("/debug/config")
def debug_config() -> dict[str, str | int]:
    return {
        "pid": os.getpid(),
        "access_key_id": mask_config_value(os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_ID", "")),
        "access_key_secret": mask_config_value(os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "")),
        "security_token": mask_config_value(os.environ.get("ALIBABA_CLOUD_SECURITY_TOKEN", "")),
        "ots_access_key_id": mask_config_value(os.environ.get("OTS_ACCESS_KEY_ID", "")),
        "ots_access_key_secret": mask_config_value(os.environ.get("OTS_ACCESS_KEY_SECRET", "")),
        "ots_security_token": mask_config_value(os.environ.get("OTS_SECURITY_TOKEN", "")),
        "tablestore_instance": os.environ.get("WQ_LEARNER_TABLESTORE_INSTANCE", ""),
        "tablestore_endpoint": os.environ.get("WQ_LEARNER_TABLESTORE_ENDPOINT", ""),
        "phone_auth_endpoint": os.environ.get("ALIYUN_PHONE_AUTH_ENDPOINT", ""),
        "oss_bucket": os.environ.get("WQ_LEARNER_OSS_BUCKET", ""),
    }


@app.get("/debug/tablestore")
def debug_tablestore() -> dict[str, str | int | bool]:
    adapter = getattr(store, "adapter", None)
    if adapter is None:
        return {"ok": False, "store": type(store).__name__, "message": "store has no TableStore adapter"}
    try:
        adapter.get_row(TABLE_USERS, [("email", "__debug_not_existing__")])
        return {"ok": True, "store": type(store).__name__, "message": "TableStore get_row succeeded"}
    except Exception as error:
        return {
            "ok": False,
            "store": type(store).__name__,
            "error_type": type(error).__name__,
            "message": str(error),
            "access_key_id": mask_config_value(os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_ID", "")),
            "access_key_secret": mask_config_value(os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "")),
            "security_token": mask_config_value(os.environ.get("ALIBABA_CLOUD_SECURITY_TOKEN", "")),
            "ots_access_key_id": mask_config_value(os.environ.get("OTS_ACCESS_KEY_ID", "")),
            "ots_access_key_secret": mask_config_value(os.environ.get("OTS_ACCESS_KEY_SECRET", "")),
            "ots_security_token": mask_config_value(os.environ.get("OTS_SECURITY_TOKEN", "")),
            "tablestore_instance": os.environ.get("WQ_LEARNER_TABLESTORE_INSTANCE", ""),
            "tablestore_endpoint": os.environ.get("WQ_LEARNER_TABLESTORE_ENDPOINT", ""),
        }


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


@app.post("/auth/sms/send", response_model=SmsCodeResponse)
def send_sms_code(request: SmsCodeRequest) -> SmsCodeResponse:
    try:
        phone = normalize_phone(request.phone)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid phone number") from None
    code = sms_code_store.issue(phone)
    try:
        sms_service.send_code(phone, code)
    except SmsConfigurationError as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
    except SmsSendError as error:
        raise HTTPException(status_code=502, detail=str(error)) from error
    return SmsCodeResponse(sent=True)


@app.post("/auth/sms/login", response_model=TokenResponse)
def login_with_sms_code(request: SmsLoginRequest) -> TokenResponse:
    try:
        phone = normalize_phone(request.phone)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid phone number") from None
    if not sms_code_store.verify(phone, request.code):
        raise HTTPException(status_code=401, detail="Invalid or expired code")
    token = store.login_or_register_phone(phone)
    return TokenResponse(access_token=token)


@app.post("/auth/phone/one-click-login", response_model=TokenResponse)
def login_with_one_click_phone(request: OneClickPhoneLoginRequest) -> TokenResponse:
    try:
        phone = phone_auth_service.mobile_for_access_token(request.access_token)
    except PhoneAuthConfigurationError as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
    except PhoneAuthError as error:
        raise HTTPException(status_code=401, detail=str(error)) from error
    token = store.login_or_register_phone(phone)
    return TokenResponse(access_token=token, account=phone)


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
        answer_md_latex=request.answer_md_latex,
        explanation_md_latex=request.explanation_md_latex,
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
