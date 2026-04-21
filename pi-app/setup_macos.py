"""macOS-only launcher bundle for AntiDonut Pi.

Builds a .app around main.py with the Info.plist keys CoreBluetooth needs
to clear TCC (the Homebrew python3 binary has no NSBluetoothAlwaysUsageDescription,
so macOS kills it with SIGABRT the moment bless tries to advertise).

For running on an actual Raspberry Pi this file is irrelevant — use
`python main.py` there directly (BlueZ has no equivalent TCC gate).

Build (alias mode — symlinks into the working tree, edits are live):

    source .venv/bin/activate
    pip install py2app
    python setup_macos.py py2app -A
    open dist/AntiDonut-Pi.app

Clean rebuild:

    rm -rf build dist && python setup_macos.py py2app -A

Reset the TCC decision (e.g. if you denied BT and want the prompt again):

    tccutil reset Bluetooth com.antidonut.antidonut.pi
"""
from setuptools import setup

setup(
    app=["main.py"],
    name="AntiDonut-Pi",
    options={
        "py2app": {
            "argv_emulation": False,
            "plist": {
                "CFBundleName": "AntiDonut-Pi",
                "CFBundleDisplayName": "AntiDonut-Pi",
                "CFBundleIdentifier": "com.antidonut.antidonut.pi",
                "CFBundleVersion": "1.0",
                "CFBundleShortVersionString": "1.0",
                "LSMinimumSystemVersion": "13.0",
                "NSBluetoothAlwaysUsageDescription":
                    "AntiDonut-Pi advertises as a BLE peripheral so your "
                    "iPhone companion app can sync profile and health data.",
                "NSBluetoothPeripheralUsageDescription":
                    "AntiDonut-Pi advertises over Bluetooth to pair with "
                    "the iOS companion.",
                "NSHighResolutionCapable": True,
                # Keep it windowed — we have a Qt GUI.
                "LSUIElement": False,
            },
            "packages": ["PyQt6", "bless"],
            "includes": ["services", "ui", "database"],
        }
    },
    setup_requires=["py2app"],
)
