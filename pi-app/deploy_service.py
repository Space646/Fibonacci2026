#!/usr/bin/env python3
"""Deploy the AntiDonut Pi app as a systemd kiosk service.

Run on the Raspberry Pi as root (or with sudo):

    sudo python3 deploy_service.py

What it does:
  1. Creates a virtualenv and installs dependencies (if needed).
  2. Writes a systemd unit file for the kiosk app.
  3. Enables and starts the service.

The service runs as the 'pi' user (override with --user), restarts
automatically on failure, and only stops when explicitly told to.
"""
import argparse
import os
import subprocess
import sys
import textwrap
from pathlib import Path

SERVICE_NAME = "antidonut-kiosk"
UNIT_PATH = Path(f"/etc/systemd/system/{SERVICE_NAME}.service")

APP_DIR = Path(__file__).resolve().parent
MAIN_PY = APP_DIR / "main.py"
VENV_DIR = APP_DIR / ".venv"
REQUIREMENTS = APP_DIR / "requirements.txt"


def run(cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
    print(f"  $ {' '.join(cmd)}")
    return subprocess.run(cmd, check=True, **kwargs)


def ensure_system_deps():
    pkgs = ["libxcb-cursor0"]
    result = subprocess.run(
        ["dpkg", "-l"] + pkgs,
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    if result.returncode != 0:
        print(f"Installing system dependencies: {' '.join(pkgs)} …")
        run(["apt-get", "install", "-y"] + pkgs)


def ensure_venv():
    python = VENV_DIR / "bin" / "python"
    if not python.exists():
        print("Creating virtualenv …")
        run([sys.executable, "-m", "venv", str(VENV_DIR)])
    print("Installing / updating dependencies …")
    pip = str(VENV_DIR / "bin" / "pip")
    run([pip, "install", "--upgrade", "pip"], stdout=subprocess.DEVNULL)
    run([pip, "install", "-r", str(REQUIREMENTS)])


def write_unit(user: str, display: str):
    python = VENV_DIR / "bin" / "python"
    xauthority = f"/home/{user}/.Xauthority"

    unit = textwrap.dedent(f"""\
        [Unit]
        Description=AntiDonut Kiosk (PyQt6)
        After=graphical.target
        Wants=graphical.target

        [Service]
        Type=simple
        User={user}
        WorkingDirectory={APP_DIR}
        Environment=DISPLAY={display}
        Environment=XAUTHORITY={xauthority}
        Environment=QT_QPA_PLATFORM=xcb
        Environment=QSG_RHI_BACKEND=opengl
        ExecStart={python} {MAIN_PY}

        Restart=always
        RestartSec=3

        StandardOutput=journal
        StandardError=journal
        SyslogIdentifier={SERVICE_NAME}

        [Install]
        WantedBy=graphical.target
    """)

    UNIT_PATH.write_text(unit)
    print(f"Wrote {UNIT_PATH}")


def write_sudoers(user: str):
    sudoers_path = Path(f"/etc/sudoers.d/antidonut-kiosk")
    systemctl = "/bin/systemctl"
    rule = (
        f"{user} ALL=(ALL) NOPASSWD: {systemctl} stop {SERVICE_NAME}\n"
        f"{user} ALL=(ALL) NOPASSWD: {systemctl} restart {SERVICE_NAME}\n"
    )
    sudoers_path.write_text(rule)
    sudoers_path.chmod(0o440)
    print(f"Wrote {sudoers_path}")


def enable_and_start():
    run(["systemctl", "daemon-reload"])
    run(["systemctl", "enable", SERVICE_NAME])
    run(["systemctl", "restart", SERVICE_NAME])
    print()
    print(f"  $ systemctl status {SERVICE_NAME} --no-pager")
    subprocess.run(["systemctl", "status", SERVICE_NAME, "--no-pager"], check=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--user", default="tadroid", help="Linux user to run the service as (default: tadroid)")
    parser.add_argument("--display", default=":0", help="X display for the GUI (default: :0)")
    parser.add_argument("--uninstall", action="store_true", help="Stop, disable, and remove the service")
    args = parser.parse_args()

    if os.geteuid() != 0:
        sys.exit("Error: this script must be run as root (use sudo).")

    if args.uninstall:
        print(f"Removing {SERVICE_NAME} …")
        subprocess.run(["systemctl", "stop", SERVICE_NAME], check=False)
        subprocess.run(["systemctl", "disable", SERVICE_NAME], check=False)
        if UNIT_PATH.exists():
            UNIT_PATH.unlink()
            run(["systemctl", "daemon-reload"])
        sudoers_path = Path("/etc/sudoers.d/antidonut-kiosk")
        if sudoers_path.exists():
            sudoers_path.unlink()
        print("Done.")
        return

    print(f"Deploying {SERVICE_NAME} from {APP_DIR}\n")

    ensure_system_deps()
    ensure_venv()
    print()
    write_unit(args.user, args.display)
    write_sudoers(args.user)
    print()
    enable_and_start()

    print(f"\nKiosk is running. Useful commands:")
    print(f"  journalctl -u {SERVICE_NAME} -f        # follow logs")
    print(f"  sudo systemctl stop {SERVICE_NAME}      # stop")
    print(f"  sudo systemctl start {SERVICE_NAME}     # start")
    print(f"  sudo python3 {__file__} --uninstall     # remove")


if __name__ == "__main__":
    main()
