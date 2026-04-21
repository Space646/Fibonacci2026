# AntiDonut Pi App

Nutrition-tracking food scale built on Raspberry Pi with a HuskyLens camera and
HX711 load cell. Pairs over BLE with the iOS companion app.

## Running

### Test mode (any platform, no hardware)

```bash
pip install -r requirements.txt
python main.py --test
```

Enables the in-app Test Mode toggle automatically, bypasses HuskyLens UART and
HX711 GPIO, and disables the BLE peripheral (use the Settings → "Simulate phone
pairing" button to exercise the connected-user UI).

### Normal mode (macOS for development)

```bash
pip install -r requirements.txt
python main.py
```

On first launch macOS asks permission to use Bluetooth — accept. The app will
advertise as `AntiDonut-Pi` and an iPhone running AntiDonut can find it from
its Device tab.

> **Known macOS limitation.** A stock Homebrew `python3` binary has no
> `NSBluetoothAlwaysUsageDescription` in its Info.plist, so macOS kills the
> process with SIGABRT the moment bless tries to advertise. Workarounds:
>
> 1. **py2app bundle** (recommended for demos) — bundle `main.py` with a
>    proper Info.plist containing the BT usage description. See
>    `docs/superpowers/specs/2026-04-14-ble-connection-completion-design.md`.
> 2. **Test iOS from a real Pi** — the macOS BLE peripheral is only used for
>    developer smoke tests; production targets Raspberry Pi OS where this
>    limitation doesn't apply.
> 3. **Run unit tests only** — `pytest -v` covers the Python surface of
>    `BluetoothServer` without needing real BLE hardware.

### Normal mode (Raspberry Pi OS)

```bash
sudo apt install bluez
pip install -r requirements.txt
python main.py
```

The default `pi` user is already in the `bluetooth` group. No systemd unit is
required; the app talks to BlueZ over the user session D-Bus bus.

## Troubleshooting

- **Python logs `BLE unavailable — import failed`**: `bless` didn't install.
  Re-run `pip install -r requirements.txt` and check for a Python version
  mismatch (bless needs 3.10+).
- **macOS: `started = False` after startup / SIGABRT on launch**: Bluetooth
  permission was denied for the Python binary, or the binary has no
  `NSBluetoothAlwaysUsageDescription` in its Info.plist (see the macOS note
  above). Grant the permission in System Settings → Privacy & Security →
  Bluetooth, or bundle the app with py2app.
- **Pi: BlueZ errors in `journalctl --user`**: Make sure `bluetoothd` is
  running (`sudo systemctl status bluetooth`) and your Pi's BT adapter is up
  (`bluetoothctl power on`).
- **iPhone never discovers the Pi**: Confirm the Pi is advertising with
  `bluetoothctl scan le` from another Linux box, or check `started = True` in
  the Pi app logs.
