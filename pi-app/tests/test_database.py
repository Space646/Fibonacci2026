import sqlite3
import pytest
from database.db import get_connection, run_migrations
from database.seed_foods import seed_foods, compute_health_score

def test_migrations_create_tables():
    conn = get_connection(":memory:")
    run_migrations(conn)
    tables = {r[0] for r in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table'"
    ).fetchall()}
    assert "foods" in tables
    assert "users" in tables
    assert "food_log" in tables

def test_foods_table_columns():
    conn = get_connection(":memory:")
    run_migrations(conn)
    cols = {r[1] for r in conn.execute("PRAGMA table_info(foods)").fetchall()}
    assert cols >= {
        "id", "name", "calories_per_100g", "protein_per_100g",
        "fat_per_100g", "sugar_per_100g", "fiber_per_100g",
        "is_healthy", "health_score", "huskylens_label_id"
    }

def test_users_table_columns():
    conn = get_connection(":memory:")
    run_migrations(conn)
    cols = {r[1] for r in conn.execute("PRAGMA table_info(users)").fetchall()}
    assert cols >= {
        "id", "bluetooth_mac", "name", "age",
        "weight_kg", "height_cm", "sex", "activity_level", "daily_calorie_goal"
    }

def test_food_log_foreign_keys():
    conn = get_connection(":memory:")
    conn.execute("PRAGMA foreign_keys = ON")
    run_migrations(conn)
    with pytest.raises(sqlite3.IntegrityError):
        conn.execute(
            "INSERT INTO food_log (user_id, food_id, weight_g, calories) VALUES (999, 999, 100, 50)"
        )
        conn.commit()

def test_seed_inserts_foods(seeded_db):
    count = seeded_db.execute("SELECT COUNT(*) FROM foods").fetchone()[0]
    assert count == 25

def test_health_score_high_fiber_protein():
    score = compute_health_score(17, 10.6, 1, 7)
    assert score > 60

def test_health_score_low_sugar_junk():
    score = compute_health_score(0, 0, 60, 50)
    assert score < 30
