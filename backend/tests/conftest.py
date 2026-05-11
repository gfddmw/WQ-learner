import os
import shutil
import sys
import tempfile
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

TEST_DB = Path(tempfile.gettempdir()) / "wq_learner_pytest.db"
if TEST_DB.exists():
    TEST_DB.unlink()
os.environ["WQ_LEARNER_DB"] = str(TEST_DB)

TEST_UPLOAD_DIR = Path(tempfile.gettempdir()) / "wq_learner_uploads"
if TEST_UPLOAD_DIR.exists():
    shutil.rmtree(TEST_UPLOAD_DIR)
os.environ["WQ_LEARNER_UPLOAD_DIR"] = str(TEST_UPLOAD_DIR)
