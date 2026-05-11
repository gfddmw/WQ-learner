from app.store import Store, TableStoreStore


def test_tablestore_store_registers_logs_in_and_manages_questions():
    store = TableStoreStore(FakeTableStoreAdapter())

    user = store.register("demo@example.com", "secret123")
    same_user = store.register("demo@example.com", "secret123")
    token = store.login("demo@example.com", "secret123")

    assert isinstance(store, Store)
    assert same_user.id == user.id
    assert token is not None
    assert store.login("demo@example.com", "wrong") is None
    assert store.user_for_token(token) == user

    draft = store.create_upload_draft(user.id, "oss://wq-learner/users/u/questions/q.jpg")
    confirmed = store.confirm_question(
        question_id=draft.id,
        user_id=user.id,
        content_md_latex="树的遍历复杂度为 $O(n)$。",
        subject="数据结构",
        chapter="树与二叉树",
        mastery="reviewing",
    )

    assert confirmed is not None
    assert confirmed.status == "confirmed"
    assert store.get_question(draft.id, user.id) == confirmed
    assert store.list_questions(user.id, subject="数据结构") == [confirmed]
    assert store.list_questions(user.id, subject="计算机网络") == []


def test_tablestore_store_manages_practice_records():
    store = TableStoreStore(FakeTableStoreAdapter())
    user = store.register("demo@example.com", "secret123")
    first = store.create_upload_draft(user.id, "oss://bucket/first.jpg")
    second = store.create_upload_draft(user.id, "oss://bucket/second.jpg")

    original = store.create_original_practice(user.id, count=1)
    variant = store.create_variant_practice(user.id, source_question_id=first.id, topic="二叉树")
    reviewed = store.review_practice(original.id, user.id, result="reviewing")

    assert original.question_ids == [second.id]
    assert store.questions_by_ids(user.id, [first.id, "missing"]) == [first]
    assert variant.variant is not None
    assert variant.variant["title"] == "模拟变形题：二叉树"
    assert reviewed is not None
    assert reviewed.result == "reviewing"


class FakeTableStoreAdapter:
    def __init__(self):
        self.rows = {}

    def put_row(self, table_name, primary_key, attributes):
        duplicated_columns = {name for name, _ in primary_key}.intersection(attributes.keys())
        if duplicated_columns:
            raise ValueError(f"duplicated primary key attributes: {duplicated_columns}")

        row = dict(attributes)
        row.update(dict(primary_key))
        self.rows[(table_name, tuple(primary_key))] = row

    def get_row(self, table_name, primary_key):
        attributes = self.rows.get((table_name, tuple(primary_key)))
        if attributes is None:
            return None
        return dict(attributes)

    def get_range(self, table_name, primary_key_prefix):
        prefix = tuple(primary_key_prefix)
        rows = []
        for (row_table_name, row_primary_key), attributes in self.rows.items():
            if row_table_name == table_name and row_primary_key[: len(prefix)] == prefix:
                rows.append(dict(attributes))
        return rows
