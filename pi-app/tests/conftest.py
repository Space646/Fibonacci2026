import pytest
from database.db import get_connection, run_migrations
from database.seed_foods import seed_foods

@pytest.fixture
def db():
    conn = get_connection(":memory:")
    run_migrations(conn)
    return conn

@pytest.fixture
def seeded_db(db):
    seed_foods(db)
    return db
