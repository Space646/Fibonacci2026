import sys
import os
from PyQt6.QtGui import QGuiApplication
from PyQt6.QtQml import QQmlApplicationEngine, qmlRegisterSingletonType
from PyQt6.QtCore import QUrl

from ui.app_state import AppState

TEST_MODE = "--test" in sys.argv
if sys.platform == "linux":
    os.environ.setdefault("QT_QPA_PLATFORM", "xcb")
    os.environ.setdefault("QSG_RHI_BACKEND", "opengl")

def main():
    app = QGuiApplication(sys.argv)
    app.setApplicationName("AntiDonut")

    state = AppState(test_mode=TEST_MODE)

    engine = QQmlApplicationEngine()
    engine.rootContext().setContextProperty("appState", state)

    qml_dir = os.path.join(os.path.dirname(__file__), "ui")
    engine.addImportPath(qml_dir)

    engine.load(QUrl.fromLocalFile(os.path.join(qml_dir, "main.qml")))

    if not engine.rootObjects():
        sys.exit(-1)

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
