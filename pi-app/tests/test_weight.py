from services.weight import WeightService


def test_test_mode_returns_set_weight():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(150.0)
    assert svc.current_weight_g == 150.0

def test_test_mode_default_zero():
    svc = WeightService(test_mode=True)
    assert svc.current_weight_g == 0.0

def test_set_test_weight_updates_value():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(200.0)
    svc.set_test_weight(99.5)
    assert svc.current_weight_g == 99.5

def test_stability_requires_five_readings():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(100.0)
    assert svc.is_stable is True

def test_hardware_mode_not_stable_initially():
    svc = WeightService(test_mode=False, skip_hardware_init=True)
    assert svc.is_stable is False
    assert svc.current_weight_g == 0.0
