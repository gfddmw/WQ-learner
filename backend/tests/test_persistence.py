from app.store import SQLiteStore


def test_sqlite_store_persists_users_tokens_and_questions(tmp_path):
    db_path = tmp_path / "wq_learner.db"
    first_store = SQLiteStore(db_path)

    user = first_store.register("persist@example.com", "secret123")
    first_token = first_store.login("persist@example.com", "secret123")
    assert first_token is not None

    draft = first_store.create_upload_draft(user.id, "/uploads/users/user/question.png")
    confirmed = first_store.confirm_question(
        question_id=draft.id,
        user_id=user.id,
        content_md_latex="Binary tree traversal costs $O(n)$.",
        subject="数据结构",
        chapter="树与二叉树",
        mastery="reviewing",
    )
    assert confirmed is not None

    second_store = SQLiteStore(db_path)
    second_token = second_store.login("persist@example.com", "secret123")
    assert second_token is not None

    reloaded_user = second_store.user_for_token(second_token)
    assert reloaded_user is not None
    questions = second_store.list_questions(reloaded_user.id, subject="数据结构")

    assert len(questions) == 1
    assert questions[0].content_md_latex == "Binary tree traversal costs $O(n)$."
    assert questions[0].status == "confirmed"
