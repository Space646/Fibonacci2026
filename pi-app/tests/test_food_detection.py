import pytest
from services.food_detection import FoodDetectionService
from database.seed_foods import seed_foods


def test_test_mode_inject_known_food(seeded_db):
    svc = FoodDetectionService(conn=seeded_db, test_mode=True)
    result = svc.inject_food_by_id(1)  # Apple
    assert result is not None
    assert result["name"] == "Apple"
    assert result["calories_per_100g"] == 52

def test_test_mode_inject_unknown_id(seeded_db):
    svc = FoodDetectionService(conn=seeded_db, test_mode=True)
    result = svc.inject_food_by_id(999)
    assert result is None

def test_lookup_by_label_id(seeded_db):
    svc = FoodDetectionService(conn=seeded_db, test_mode=True)
    result = svc._lookup_label(2)  # Banana label_id=2
    assert result is not None
    assert result["name"] == "Banana"

def test_lookup_unmapped_label(seeded_db):
    svc = FoodDetectionService(conn=seeded_db, test_mode=True)
    result = svc._lookup_label(999)
    assert result is None

def test_get_all_foods_returns_list(seeded_db):
    svc = FoodDetectionService(conn=seeded_db, test_mode=True)
    foods = svc.get_all_foods()
    assert len(foods) == 25
    assert all("name" in f for f in foods)
