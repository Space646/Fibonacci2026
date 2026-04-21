# Database Editor

Small Flask web app to edit the `foods` database and export it as a Python `FOODS` list.

Quick start (macOS, zsh):

```bash
cd "database-editor"
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
# open http://127.0.0.1:5000
```

Production / containerized

This app stores the entire database in the browser (client-side) using localStorage. There is no server-side database file — the container simply serves the static frontend.

Build and run with Docker (detached/background):

```bash
cd "database-editor"
docker build -t db-editor .
# run container in background (detached)
docker run -d --name db-editor -p 8000:8000 -e PORT=8000 db-editor
# open http://127.0.0.1:8000
```

To stop or remove the background container:

```bash
docker stop db-editor
docker rm db-editor
```

Or using docker-compose (detached with automatic restart):

```bash
docker-compose up --build -d
```

To view logs from the running container:

```bash
docker logs -f db-editor
```

To stop the compose stack:

```bash
docker-compose down
```

Features:
- View all foods
- Add, edit, delete entries
- Export the data as a Python `FOODS = [...]` literal

Storage: client-side localStorage (no server-side DB). The app seeds example foods on first run in each browser.
