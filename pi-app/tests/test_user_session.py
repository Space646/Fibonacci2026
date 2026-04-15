import pytest
from services.user_session import UserSessionManager
from database.seed_foods import seed_foods

GUEST_MAC = "00:00:00:00:00:00"

def test_upsert_creates_new_user(seeded_db):
    mgr = UserSessionManager(seeded_db)
    user = mgr.upsert_user("AA:BB:CC:DD:EE:FF", {"name": "Alice", "age": 25,
        "weight_kg": 60, "height_cm": 165, "sex": "female",
        "activity_level": "moderate", "daily_calorie_goal": None})
    assert user["id"] is not None
    assert user["name"] == "Alice"

def test_upsert_updates_existing_user(seeded_db):
    mgr = UserSessionManager(seeded_db)
    mgr.upsert_user("AA:BB:CC:DD:EE:FF", {"name": "Alice", "age": 25,
        "weight_kg": 60, "height_cm": 165, "sex": "female",
        "activity_level": "moderate", "daily_calorie_goal": None})
    user = mgr.upsert_user("AA:BB:CC:DD:EE:FF", {"name": "Alice Updated",
        "age": 26, "weight_kg": 61, "height_cm": 165, "sex": "female",
        "activity_level": "active", "daily_calorie_goal": 2200.0})
    assert user["name"] == "Alice Updated"
    assert user["daily_calorie_goal"] == 2200.0

def test_log_food_stores_entry(seeded_db):
    mgr = UserSessionManager(seeded_db)
    user = mgr.upsert_user(GUEST_MAC, {"name": None, "age": None,
        "weight_kg": None, "height_cm": None, "sex": "other",
        "activity_level": "sedentary", "daily_calorie_goal": None})
    entry = mgr.log_food(user["id"], food_id=1, weight_g=182, calories=94.6)
    assert entry["food_id"] == 1
    assert entry["weight_g"] == 182

def test_get_todays_log(seeded_db):
    mgr = UserSessionManager(seeded_db)
    user = mgr.upsert_user(GUEST_MAC, {"name": None, "age": None,
        "weight_kg": None, "height_cm": None, "sex": "other",
        "activity_level": "sedentary", "daily_calorie_goal": None})
    mgr.log_food(user["id"], food_id=1, weight_g=100, calories=52)
    mgr.log_food(user["id"], food_id=2, weight_g=120, calories=106.8)
    log = mgr.get_todays_log(user["id"])
    assert len(log) == 2

def test_total_calories_today(seeded_db):
    mgr = UserSessionManager(seeded_db)
    user = mgr.upsert_user(GUEST_MAC, {"name": None, "age": None,
        "weight_kg": None, "height_cm": None, "sex": "other",
        "activity_level": "sedentary", "daily_calorie_goal": None})
    mgr.log_food(user["id"], food_id=1, weight_g=100, calories=52)
    mgr.log_food(user["id"], food_id=2, weight_g=100, calories=89)
    assert mgr.total_calories_today(user["id"]) == pytest.approx(141)

def test_delete_log_entry(seeded_db):
    mgr = UserSessionManager(seeded_db)
    user = mgr.upsert_user(GUEST_MAC, {"name": None, "age": None,
        "weight_kg": None, "height_cm": None, "sex": "other",
        "activity_level": "sedentary", "daily_calorie_goal": None})
    entry = mgr.log_food(user["id"], food_id=1, weight_g=100, calories=52)
    mgr.delete_log_entry(entry["id"])
    assert mgr.get_todays_log(user["id"]) == []

def test_get_todays_log_includes_macro_grams(seeded_db):
    """Each entry must carry per-entry gram values for protein/fat/sugar/fiber,
    computed as (per_100g * weight_g / 100). iOS uses these to build HealthKit
    correlation samples; missing keys would cause those samples to be omitted."""
    mgr = UserSessionManager(seeded_db)
    user = mgr.upsert_user(GUEST_MAC, {"name": None, "age": None,
        "weight_kg": None, "height_cm": None, "sex": "other",
        "activity_level": "sedentary", "daily_calorie_goal": None})
    # Seed: food_id=1 is "Apple" (0.3g protein, 0.2g fat, 10.0g sugar, 2.4g fiber per 100g).
    # Log 200g of apple.
    mgr.log_food(user["id"], food_id=1, weight_g=200.0, calories=104.0)

    rows = mgr.get_todays_log(user["id"])
    assert len(rows) == 1
    row = rows[0]
    # Per-entry grams: (per_100g * weight_g / 100), rounded to 2dp
    assert row["protein_g"] == 0.6    # 0.3 * 200 / 100
    assert row["fat_g"]     == 0.4    # 0.2 * 200 / 100
    assert row["sugar_g"]   == 20.0   # 10.0 * 200 / 100
    assert row["fiber_g"]   == 4.8    # 2.4 * 200 / 100
