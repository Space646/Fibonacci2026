import sqlite3
from pathlib import Path

SCHEMA_PATH = Path(__file__).parent / "schema.sql"
DEFAULT_DB_PATH = Path(__file__).parent.parent / "data" / "fibonacci.db"


def get_connection(path: str = None) -> sqlite3.Connection:
    """Return a sqlite3 connection. Pass ':memory:' for tests."""
    db_path = path or str(DEFAULT_DB_PATH)
    if db_path != ":memory:":
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(db_path)
    conn.execute("PRAGMA foreign_keys = ON")
    conn.row_factory = sqlite3.Row
    return conn


def run_migrations(conn: sqlite3.Connection) -> None:
    """Create tables from schema.sql if they don't exist."""
    schema = SCHEMA_PATH.read_text()
    conn.executescript(schema)
    conn.commit()
