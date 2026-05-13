import json
import os
import sqlite3
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Protocol, runtime_checkable
from uuid import uuid4

from .classifier import classify_question


TABLE_USERS = "wq_users"
TABLE_TOKENS = "wq_tokens"
TABLE_QUESTIONS = "wq_questions"
TABLE_PRACTICES = "wq_practices"


@dataclass
class UserRecord:
    id: str
    email: str
    password: str


@dataclass
class QuestionRecord:
    id: str
    user_id: str
    image_url: str
    content_md_latex: str
    subject: str
    chapter: str
    status: str = "draft"
    mastery: str = "unfamiliar"
    answer_md_latex: str = ""
    explanation_md_latex: str = ""


@dataclass
class PracticeRecord:
    id: str
    user_id: str
    mode: str
    question_ids: list[str] = field(default_factory=list)
    variant: dict | None = None
    result: str | None = None


@runtime_checkable
class Store(Protocol):
    def register(self, email: str, password: str) -> UserRecord:
        ...

    def login(self, email: str, password: str) -> str | None:
        ...

    def login_or_register_phone(self, phone: str) -> str:
        ...

    def user_for_token(self, token: str) -> UserRecord | None:
        ...

    def create_upload_draft(
        self,
        user_id: str,
        image_url: str,
        content_md_latex: str | None = None,
        subject: str | None = None,
        chapter: str | None = None,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord:
        ...

    def confirm_question(
        self,
        question_id: str,
        user_id: str,
        content_md_latex: str,
        subject: str,
        chapter: str,
        mastery: str,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord | None:
        ...

    def get_question(self, question_id: str, user_id: str) -> QuestionRecord | None:
        ...

    def list_questions(
        self,
        user_id: str,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> list[QuestionRecord]:
        ...

    def create_original_practice(
        self,
        user_id: str,
        count: int,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> PracticeRecord:
        ...

    def create_variant_practice(
        self,
        user_id: str,
        source_question_id: str,
        topic: str,
        variant: dict | None = None,
    ) -> PracticeRecord:
        ...

    def review_practice(self, practice_id: str, user_id: str, result: str) -> PracticeRecord | None:
        ...

    def questions_by_ids(self, user_id: str, question_ids: list[str]) -> list[QuestionRecord]:
        ...


class SQLiteStore:
    def __init__(self, db_path: str | Path) -> None:
        self.db_path = Path(db_path)
        if self.db_path != Path(":memory:"):
            self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._initialize_schema()

    def _connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self.db_path)
        connection.row_factory = sqlite3.Row
        return connection

    def _initialize_schema(self) -> None:
        with self._connect() as connection:
            connection.executescript(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS tokens (
                    token TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES users(id)
                );

                CREATE TABLE IF NOT EXISTS questions (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES users(id),
                    image_url TEXT NOT NULL,
                    content_md_latex TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    chapter TEXT NOT NULL,
                    status TEXT NOT NULL,
                    mastery TEXT NOT NULL,
                    answer_md_latex TEXT,
                    explanation_md_latex TEXT
                );

                CREATE TABLE IF NOT EXISTS practices (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES users(id),
                    mode TEXT NOT NULL,
                    question_ids_json TEXT NOT NULL,
                    variant_json TEXT,
                    result TEXT
                );
                """
            )

    def register(self, email: str, password: str) -> UserRecord:
        existing = self._user_by_email(email)
        if existing is not None:
            return existing

        user = UserRecord(id=str(uuid4()), email=email, password=password)
        with self._connect() as connection:
            connection.execute(
                "INSERT INTO users (id, email, password) VALUES (?, ?, ?)",
                (user.id, user.email, user.password),
            )
        return user

    def login(self, email: str, password: str) -> str | None:
        user = self._user_by_email(email)
        if user is None or user.password != password:
            return None

        return self._create_token(user)

    def login_or_register_phone(self, phone: str) -> str:
        user = self._user_by_email(phone)
        if user is None:
            user = UserRecord(id=str(uuid4()), email=phone, password="")
            with self._connect() as connection:
                connection.execute(
                    "INSERT INTO users (id, email, password) VALUES (?, ?, ?)",
                    (user.id, user.email, user.password),
                )
        return self._create_token(user)

    def _create_token(self, user: UserRecord) -> str:
        token = str(uuid4())
        with self._connect() as connection:
            connection.execute(
                "INSERT INTO tokens (token, user_id) VALUES (?, ?)",
                (token, user.id),
            )
        return token

    def user_for_token(self, token: str) -> UserRecord | None:
        with self._connect() as connection:
            row = connection.execute(
                """
                SELECT users.id, users.email, users.password
                FROM tokens
                JOIN users ON users.id = tokens.user_id
                WHERE tokens.token = ?
                """,
                (token,),
            ).fetchone()
        return self._row_to_user(row)

    def create_upload_draft(
        self,
        user_id: str,
        image_url: str,
        content_md_latex: str | None = None,
        subject: str | None = None,
        chapter: str | None = None,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord:
        content_md_latex, subject, chapter = draft_fields_or_default(
            content_md_latex,
            subject,
            chapter,
        )
        question = QuestionRecord(
            id=str(uuid4()),
            user_id=user_id,
            image_url=image_url,
            content_md_latex=content_md_latex,
            subject=subject,
            chapter=chapter,
            answer_md_latex=answer_md_latex or "",
            explanation_md_latex=explanation_md_latex or "",
        )
        self._save_question(question)
        return question

    def confirm_question(
        self,
        question_id: str,
        user_id: str,
        content_md_latex: str,
        subject: str,
        chapter: str,
        mastery: str,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord | None:
        question = self.get_question(question_id, user_id)
        if question is None:
            return None

        question.content_md_latex = content_md_latex
        question.subject = subject
        question.chapter = chapter
        question.mastery = mastery
        if answer_md_latex is not None:
            question.answer_md_latex = answer_md_latex
        if explanation_md_latex is not None:
            question.explanation_md_latex = explanation_md_latex
        question.status = "confirmed"
        self._save_question(question)
        return question

    def get_question(self, question_id: str, user_id: str) -> QuestionRecord | None:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT * FROM questions WHERE id = ? AND user_id = ?",
                (question_id, user_id),
            ).fetchone()
        return self._row_to_question(row)

    def list_questions(
        self,
        user_id: str,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> list[QuestionRecord]:
        query = "SELECT * FROM questions WHERE user_id = ? AND status = 'confirmed'"
        params: list[str] = [user_id]
        if subject:
            query += " AND subject = ?"
            params.append(subject)
        if chapter:
            query += " AND chapter = ?"
            params.append(chapter)
        query += " ORDER BY rowid DESC"

        with self._connect() as connection:
            rows = connection.execute(query, params).fetchall()
        return [self._row_to_question(row) for row in rows if row is not None]

    def create_original_practice(
        self,
        user_id: str,
        count: int,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> PracticeRecord:
        questions = self.list_questions(user_id, subject, chapter)[:count]
        practice = PracticeRecord(
            id=str(uuid4()),
            user_id=user_id,
            mode="original",
            question_ids=[question.id for question in questions],
        )
        self._save_practice(practice)
        return practice

    def create_variant_practice(
        self,
        user_id: str,
        source_question_id: str,
        topic: str,
        variant: dict | None = None,
    ) -> PracticeRecord:
        variant = variant or default_variant(source_question_id, topic)
        practice = PracticeRecord(
            id=str(uuid4()),
            user_id=user_id,
            mode="variant",
            question_ids=[],
            variant=variant,
        )
        self._save_practice(practice)
        return practice

    def review_practice(self, practice_id: str, user_id: str, result: str) -> PracticeRecord | None:
        practice = self.get_practice(practice_id, user_id)
        if practice is None:
            return None
        practice.result = result
        self._save_practice(practice)
        return practice

    def get_practice(self, practice_id: str, user_id: str) -> PracticeRecord | None:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT * FROM practices WHERE id = ? AND user_id = ?",
                (practice_id, user_id),
            ).fetchone()
        return self._row_to_practice(row)

    def questions_by_ids(self, user_id: str, question_ids: list[str]) -> list[QuestionRecord]:
        return [
            question
            for question_id in question_ids
            if (question := self.get_question(question_id, user_id)) is not None
        ]

    def _user_by_email(self, email: str) -> UserRecord | None:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT id, email, password FROM users WHERE email = ?",
                (email,),
            ).fetchone()
        return self._row_to_user(row)

    def _save_question(self, question: QuestionRecord) -> None:
        with self._connect() as connection:
            connection.execute(
                """
                INSERT INTO questions (
                    id, user_id, image_url, content_md_latex,
                    subject, chapter, status, mastery,
                    answer_md_latex, explanation_md_latex
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    image_url = excluded.image_url,
                    content_md_latex = excluded.content_md_latex,
                    subject = excluded.subject,
                    chapter = excluded.chapter,
                    status = excluded.status,
                    mastery = excluded.mastery,
                    answer_md_latex = excluded.answer_md_latex,
                    explanation_md_latex = excluded.explanation_md_latex
                """,
                (
                    question.id,
                    question.user_id,
                    question.image_url,
                    question.content_md_latex,
                    question.subject,
                    question.chapter,
                    question.status,
                    question.mastery,
                    question.answer_md_latex,
                    question.explanation_md_latex,
                ),
            )

    def _save_practice(self, practice: PracticeRecord) -> None:
        with self._connect() as connection:
            connection.execute(
                """
                INSERT INTO practices (
                    id, user_id, mode, question_ids_json, variant_json, result
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    question_ids_json = excluded.question_ids_json,
                    variant_json = excluded.variant_json,
                    result = excluded.result
                """,
                (
                    practice.id,
                    practice.user_id,
                    practice.mode,
                    json.dumps(practice.question_ids, ensure_ascii=False),
                    json.dumps(practice.variant, ensure_ascii=False) if practice.variant is not None else None,
                    practice.result,
                ),
            )

    @staticmethod
    def _row_to_user(row: sqlite3.Row | None) -> UserRecord | None:
        if row is None:
            return None
        return UserRecord(id=row["id"], email=row["email"], password=row["password"])

    @staticmethod
    def _row_to_question(row: sqlite3.Row | None) -> QuestionRecord | None:
        if row is None:
            return None
        return QuestionRecord(
            id=row["id"],
            user_id=row["user_id"],
            image_url=row["image_url"],
            content_md_latex=row["content_md_latex"],
            subject=row["subject"],
            chapter=row["chapter"],
            status=row["status"],
            mastery=row["mastery"],
            answer_md_latex=row["answer_md_latex"] or "",
            explanation_md_latex=row["explanation_md_latex"] or "",
        )

    @staticmethod
    def _row_to_practice(row: sqlite3.Row | None) -> PracticeRecord | None:
        if row is None:
            return None
        variant_json = row["variant_json"]
        return PracticeRecord(
            id=row["id"],
            user_id=row["user_id"],
            mode=row["mode"],
            question_ids=json.loads(row["question_ids_json"]),
            variant=json.loads(variant_json) if variant_json else None,
            result=row["result"],
        )


@runtime_checkable
class TableStoreAdapter(Protocol):
    def put_row(self, table_name: str, primary_key: list[tuple[str, str]], attributes: dict[str, Any]) -> None:
        ...

    def get_row(self, table_name: str, primary_key: list[tuple[str, str]]) -> dict[str, Any] | None:
        ...

    def get_range(self, table_name: str, primary_key_prefix: list[tuple[str, str]]) -> list[dict[str, Any]]:
        ...


class AliyunTableStoreAdapter:
    def __init__(self, endpoint: str, instance_name: str) -> None:
        try:
            import tablestore
        except ImportError as error:
            raise RuntimeError("缺少 tablestore 依赖，请先安装 backend/requirements.txt") from error

        explicit_access_key_id = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_ID")
        explicit_access_key_secret = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_SECRET")
        access_key_id = explicit_access_key_id or os.environ.get("OTS_ACCESS_KEY_ID")
        access_key_secret = explicit_access_key_secret or os.environ.get("OTS_ACCESS_KEY_SECRET")
        security_token = ""
        if not (explicit_access_key_id and explicit_access_key_secret):
            security_token = os.environ.get("ALIBABA_CLOUD_SECURITY_TOKEN") or os.environ.get("OTS_SECURITY_TOKEN") or ""
        if not access_key_id or not access_key_secret:
            raise RuntimeError("缺少表格存储访问凭证，请为函数计算绑定 RAM 角色或配置访问密钥环境变量")

        self.tablestore = tablestore
        self.client = tablestore.OTSClient(
            endpoint,
            access_key_id,
            access_key_secret,
            instance_name,
            sts_token=security_token,
        )

    def put_row(self, table_name: str, primary_key: list[tuple[str, str]], attributes: dict[str, Any]) -> None:
        row = self.tablestore.Row(primary_key, list(attributes.items()))
        condition = self.tablestore.Condition(self.tablestore.RowExistenceExpectation.IGNORE)
        self.client.put_row(table_name, row, condition)

    def get_row(self, table_name: str, primary_key: list[tuple[str, str]]) -> dict[str, Any] | None:
        response = self.client.get_row(table_name, primary_key, max_version=1)
        row = _tablestore_response_row(response)
        return _tablestore_row_to_attributes(row)

    def get_range(self, table_name: str, primary_key_prefix: list[tuple[str, str]]) -> list[dict[str, Any]]:
        start_primary_key = primary_key_prefix + [("id", self.tablestore.INF_MIN)]
        end_primary_key = primary_key_prefix + [("id", self.tablestore.INF_MAX)]
        inclusive_start_primary_key = start_primary_key
        exclusive_end_primary_key = end_primary_key
        response = self.client.get_range(
            table_name,
            self.tablestore.Direction.FORWARD,
            inclusive_start_primary_key,
            exclusive_end_primary_key,
            limit=100,
            max_version=1,
        )
        rows = _tablestore_response_rows(response)
        return [
            attributes
            for row in rows
            if (attributes := _tablestore_row_to_attributes(row)) is not None
        ]


class TableStoreStore:
    def __init__(self, adapter: TableStoreAdapter) -> None:
        self.adapter = adapter

    def register(self, email: str, password: str) -> UserRecord:
        existing = self._user_by_email(email)
        if existing is not None:
            return existing

        user = UserRecord(id=str(uuid4()), email=email, password=password)
        self.adapter.put_row(
            TABLE_USERS,
            self._user_pk(email),
            {"id": user.id, "password": user.password},
        )
        return user

    def login(self, email: str, password: str) -> str | None:
        user = self._user_by_email(email)
        if user is None or user.password != password:
            return None

        return self._create_token(user)

    def login_or_register_phone(self, phone: str) -> str:
        user = self._user_by_email(phone)
        if user is None:
            user = UserRecord(id=str(uuid4()), email=phone, password="")
            self.adapter.put_row(
                TABLE_USERS,
                self._user_pk(user.email),
                {"id": user.id, "password": user.password},
            )
        return self._create_token(user)

    def _create_token(self, user: UserRecord) -> str:
        token = str(uuid4())
        self.adapter.put_row(
            TABLE_TOKENS,
            self._token_pk(token),
            {"user_id": user.id, "email": user.email},
        )
        return token

    def user_for_token(self, token: str) -> UserRecord | None:
        row = self.adapter.get_row(TABLE_TOKENS, self._token_pk(token))
        if row is None:
            return None
        email = str(row.get("email", ""))
        if not email:
            return None
        return self._user_by_email(email)

    def create_upload_draft(
        self,
        user_id: str,
        image_url: str,
        content_md_latex: str | None = None,
        subject: str | None = None,
        chapter: str | None = None,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord:
        content_md_latex, subject, chapter = draft_fields_or_default(
            content_md_latex,
            subject,
            chapter,
        )
        question = QuestionRecord(
            id=str(uuid4()),
            user_id=user_id,
            image_url=image_url,
            content_md_latex=content_md_latex,
            subject=subject,
            chapter=chapter,
            answer_md_latex=answer_md_latex or "",
            explanation_md_latex=explanation_md_latex or "",
        )
        self._save_question(question)
        return question

    def confirm_question(
        self,
        question_id: str,
        user_id: str,
        content_md_latex: str,
        subject: str,
        chapter: str,
        mastery: str,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord | None:
        question = self.get_question(question_id, user_id)
        if question is None:
            return None

        question.content_md_latex = content_md_latex
        question.subject = subject
        question.chapter = chapter
        question.mastery = mastery
        if answer_md_latex is not None:
            question.answer_md_latex = answer_md_latex
        if explanation_md_latex is not None:
            question.explanation_md_latex = explanation_md_latex
        question.status = "confirmed"
        self._save_question(question)
        return question

    def get_question(self, question_id: str, user_id: str) -> QuestionRecord | None:
        row = self.adapter.get_row(TABLE_QUESTIONS, self._question_pk(user_id, question_id))
        return self._row_to_question(row)

    def list_questions(
        self,
        user_id: str,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> list[QuestionRecord]:
        rows = self.adapter.get_range(TABLE_QUESTIONS, [("user_id", user_id)])
        questions = [
            question
            for row in rows
            if (question := self._row_to_question(row)) is not None and question.status == "confirmed"
        ]
        if subject:
            questions = [question for question in questions if question.subject == subject]
        if chapter:
            questions = [question for question in questions if question.chapter == chapter]
        return list(reversed(questions))

    def create_original_practice(
        self,
        user_id: str,
        count: int,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> PracticeRecord:
        questions = self.list_questions(user_id, subject, chapter)[:count]
        practice = PracticeRecord(
            id=str(uuid4()),
            user_id=user_id,
            mode="original",
            question_ids=[question.id for question in questions],
        )
        self._save_practice(practice)
        return practice

    def create_variant_practice(
        self,
        user_id: str,
        source_question_id: str,
        topic: str,
        variant: dict | None = None,
    ) -> PracticeRecord:
        variant = variant or default_variant(source_question_id, topic)
        practice = PracticeRecord(
            id=str(uuid4()),
            user_id=user_id,
            mode="variant",
            question_ids=[],
            variant=variant,
        )
        self._save_practice(practice)
        return practice

    def review_practice(self, practice_id: str, user_id: str, result: str) -> PracticeRecord | None:
        practice = self.get_practice(practice_id, user_id)
        if practice is None:
            return None
        practice.result = result
        self._save_practice(practice)
        return practice

    def get_practice(self, practice_id: str, user_id: str) -> PracticeRecord | None:
        row = self.adapter.get_row(TABLE_PRACTICES, self._practice_pk(user_id, practice_id))
        return self._row_to_practice(row)

    def questions_by_ids(self, user_id: str, question_ids: list[str]) -> list[QuestionRecord]:
        return [
            question
            for question_id in question_ids
            if (question := self.get_question(question_id, user_id)) is not None
        ]

    def _user_by_email(self, email: str) -> UserRecord | None:
        row = self.adapter.get_row(TABLE_USERS, self._user_pk(email))
        if row is None:
            return None
        return UserRecord(id=str(row["id"]), email=str(row["email"]), password=str(row["password"]))

    def _save_question(self, question: QuestionRecord) -> None:
        self.adapter.put_row(
            TABLE_QUESTIONS,
            self._question_pk(question.user_id, question.id),
            {
                "image_url": question.image_url,
                "content_md_latex": question.content_md_latex,
                "subject": question.subject,
                "chapter": question.chapter,
                "status": question.status,
                "mastery": question.mastery,
                "answer_md_latex": question.answer_md_latex,
                "explanation_md_latex": question.explanation_md_latex,
            },
        )

    def _save_practice(self, practice: PracticeRecord) -> None:
        self.adapter.put_row(
            TABLE_PRACTICES,
            self._practice_pk(practice.user_id, practice.id),
            {
                "mode": practice.mode,
                "question_ids_json": json.dumps(practice.question_ids, ensure_ascii=False),
                "variant_json": json.dumps(practice.variant, ensure_ascii=False) if practice.variant is not None else "",
                "result": practice.result or "",
            },
        )

    @staticmethod
    def _user_pk(email: str) -> list[tuple[str, str]]:
        return [("email", email)]

    @staticmethod
    def _token_pk(token: str) -> list[tuple[str, str]]:
        return [("token", token)]

    @staticmethod
    def _question_pk(user_id: str, question_id: str) -> list[tuple[str, str]]:
        return [("user_id", user_id), ("id", question_id)]

    @staticmethod
    def _practice_pk(user_id: str, practice_id: str) -> list[tuple[str, str]]:
        return [("user_id", user_id), ("id", practice_id)]

    @staticmethod
    def _row_to_question(row: dict[str, Any] | None) -> QuestionRecord | None:
        if row is None:
            return None
        return QuestionRecord(
            id=str(row["id"]),
            user_id=str(row["user_id"]),
            image_url=str(row["image_url"]),
            content_md_latex=str(row["content_md_latex"]),
            subject=str(row["subject"]),
            chapter=str(row["chapter"]),
            status=str(row.get("status") or "draft"),
            mastery=str(row.get("mastery") or "unfamiliar"),
            answer_md_latex=str(row.get("answer_md_latex") or ""),
            explanation_md_latex=str(row.get("explanation_md_latex") or ""),
        )

    @staticmethod
    def _row_to_practice(row: dict[str, Any] | None) -> PracticeRecord | None:
        if row is None:
            return None
        variant_json = str(row.get("variant_json") or "")
        return PracticeRecord(
            id=str(row["id"]),
            user_id=str(row["user_id"]),
            mode=str(row["mode"]),
            question_ids=json.loads(str(row["question_ids_json"])),
            variant=json.loads(variant_json) if variant_json else None,
            result=str(row.get("result") or "") or None,
        )


class CloudDatabaseStore:
    def __init__(self, database_url: str) -> None:
        self.database_url = database_url

    def _not_implemented(self) -> None:
        raise NotImplementedError("云端数据库适配层已预留，真实云数据库实现将在后续功能接入")

    def register(self, email: str, password: str) -> UserRecord:
        self._not_implemented()

    def login(self, email: str, password: str) -> str | None:
        self._not_implemented()

    def user_for_token(self, token: str) -> UserRecord | None:
        self._not_implemented()

    def create_upload_draft(
        self,
        user_id: str,
        image_url: str,
        content_md_latex: str | None = None,
        subject: str | None = None,
        chapter: str | None = None,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord:
        self._not_implemented()

    def confirm_question(
        self,
        question_id: str,
        user_id: str,
        content_md_latex: str,
        subject: str,
        chapter: str,
        mastery: str,
        answer_md_latex: str | None = None,
        explanation_md_latex: str | None = None,
    ) -> QuestionRecord | None:
        self._not_implemented()

    def get_question(self, question_id: str, user_id: str) -> QuestionRecord | None:
        self._not_implemented()

    def list_questions(
        self,
        user_id: str,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> list[QuestionRecord]:
        self._not_implemented()

    def create_original_practice(
        self,
        user_id: str,
        count: int,
        subject: str | None = None,
        chapter: str | None = None,
    ) -> PracticeRecord:
        self._not_implemented()

    def create_variant_practice(
        self,
        user_id: str,
        source_question_id: str,
        topic: str,
        variant: dict | None = None,
    ) -> PracticeRecord:
        self._not_implemented()

    def review_practice(self, practice_id: str, user_id: str, result: str) -> PracticeRecord | None:
        self._not_implemented()

    def questions_by_ids(self, user_id: str, question_ids: list[str]) -> list[QuestionRecord]:
        self._not_implemented()


def draft_fields_or_default(
    content_md_latex: str | None,
    subject: str | None,
    chapter: str | None,
) -> tuple[str, str, str]:
    if content_md_latex and subject and chapter:
        return content_md_latex, subject, chapter

    recognized = (
        "二叉树遍历与哈希查找综合题。请分析遍历过程，"
        "并写出平均时间复杂度 $O(n)$。"
    )
    classification = classify_question(recognized)
    return recognized, classification.subject, classification.chapter


def default_variant(source_question_id: str, topic: str) -> dict[str, str]:
    return {
        "source_question_id": source_question_id,
        "title": f"模拟变形题：{topic}",
        "content_md_latex": f"基于原错题 `{source_question_id}`，请重新分析 {topic} 的关键步骤。",
        "answer_md_latex": "这是第一版模拟答案，未来会由大模型生成。",
        "explanation_md_latex": "模拟解析用于本地开发；真实环境会由大模型生成。",
    }


def _tablestore_response_row(response: Any) -> Any:
    if response is None:
        return None
    if hasattr(response, "row"):
        return response.row
    if isinstance(response, tuple) and len(response) >= 2:
        return response[1]
    return response


def _tablestore_response_rows(response: Any) -> list[Any]:
    if response is None:
        return []
    if hasattr(response, "rows"):
        return list(response.rows)
    if hasattr(response, "row_list"):
        return list(response.row_list)
    if isinstance(response, tuple) and len(response) >= 2:
        if len(response) >= 3:
            return list(response[2] or [])
        return list(response[1] or [])
    return list(response or [])


def _tablestore_row_to_attributes(row: Any) -> dict[str, Any] | None:
    if row is None:
        return None
    if isinstance(row, dict):
        return dict(row)
    attributes: dict[str, Any] = {}
    primary_key = getattr(row, "primary_key", None)
    if primary_key:
        attributes.update(_tablestore_columns_to_dict(primary_key))
    attribute_columns = getattr(row, "attribute_columns", None)
    if attribute_columns:
        attributes.update(_tablestore_columns_to_dict(attribute_columns))
    if not attributes and isinstance(row, tuple) and len(row) >= 2:
        attributes.update(_tablestore_columns_to_dict(row[0]))
        attributes.update(_tablestore_columns_to_dict(row[1]))
    return attributes or None


def _tablestore_columns_to_dict(columns: Any) -> dict[str, Any]:
    result = {}
    for column in columns:
        if len(column) >= 2:
            result[str(column[0])] = column[1]
    return result


def create_tablestore_adapter(instance_name: str, endpoint: str) -> TableStoreAdapter:
    return AliyunTableStoreAdapter(endpoint=endpoint, instance_name=instance_name)


def default_db_path() -> Path:
    configured = os.environ.get("WQ_LEARNER_DB")
    if configured:
        return Path(configured)
    return Path(__file__).resolve().parents[1] / "data" / "wq_learner.db"


def create_store() -> Store:
    tablestore_instance = os.environ.get("WQ_LEARNER_TABLESTORE_INSTANCE")
    tablestore_endpoint = os.environ.get("WQ_LEARNER_TABLESTORE_ENDPOINT")
    if tablestore_instance and tablestore_endpoint:
        return TableStoreStore(
            create_tablestore_adapter(
                instance_name=tablestore_instance,
                endpoint=tablestore_endpoint,
            )
        )

    database_url = os.environ.get("WQ_LEARNER_DATABASE_URL")
    if database_url:
        return CloudDatabaseStore(database_url)
    return SQLiteStore(default_db_path())


store: Store = create_store()
