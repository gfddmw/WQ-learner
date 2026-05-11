import json
import os
import sqlite3
from dataclasses import dataclass, field
from pathlib import Path
from typing import Protocol, runtime_checkable
from uuid import uuid4

from .classifier import classify_question


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

    def user_for_token(self, token: str) -> UserRecord | None:
        ...

    def create_upload_draft(self, user_id: str, filename: str) -> QuestionRecord:
        ...

    def confirm_question(
        self,
        question_id: str,
        user_id: str,
        content_md_latex: str,
        subject: str,
        chapter: str,
        mastery: str,
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
                    mastery TEXT NOT NULL
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

    def create_upload_draft(self, user_id: str, filename: str) -> QuestionRecord:
        recognized = (
            "二叉树遍历与哈希查找综合题。请分析遍历过程，"
            "并写出平均时间复杂度 $O(n)$。"
        )
        classification = classify_question(recognized)
        question = QuestionRecord(
            id=str(uuid4()),
            user_id=user_id,
            image_url=f"/uploads/{filename}",
            content_md_latex=recognized,
            subject=classification.subject,
            chapter=classification.chapter,
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
    ) -> QuestionRecord | None:
        question = self.get_question(question_id, user_id)
        if question is None:
            return None

        question.content_md_latex = content_md_latex
        question.subject = subject
        question.chapter = chapter
        question.mastery = mastery
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
        query = "SELECT * FROM questions WHERE user_id = ?"
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
    ) -> PracticeRecord:
        variant = {
            "title": f"模拟变形题：{topic}",
            "content_md_latex": f"基于原错题 `{source_question_id}`，请重新分析 {topic} 的关键步骤。",
            "answer_md_latex": "这是第一版模拟答案，未来会由大模型生成。",
        }
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
                    subject, chapter, status, mastery
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    image_url = excluded.image_url,
                    content_md_latex = excluded.content_md_latex,
                    subject = excluded.subject,
                    chapter = excluded.chapter,
                    status = excluded.status,
                    mastery = excluded.mastery
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

    def create_upload_draft(self, user_id: str, filename: str) -> QuestionRecord:
        self._not_implemented()

    def confirm_question(
        self,
        question_id: str,
        user_id: str,
        content_md_latex: str,
        subject: str,
        chapter: str,
        mastery: str,
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
    ) -> PracticeRecord:
        self._not_implemented()

    def review_practice(self, practice_id: str, user_id: str, result: str) -> PracticeRecord | None:
        self._not_implemented()

    def questions_by_ids(self, user_id: str, question_ids: list[str]) -> list[QuestionRecord]:
        self._not_implemented()


def default_db_path() -> Path:
    configured = os.environ.get("WQ_LEARNER_DB")
    if configured:
        return Path(configured)
    return Path(__file__).resolve().parents[1] / "data" / "wq_learner.db"


def create_store() -> Store:
    database_url = os.environ.get("WQ_LEARNER_DATABASE_URL")
    if database_url:
        return CloudDatabaseStore(database_url)
    return SQLiteStore(default_db_path())


store: Store = create_store()
