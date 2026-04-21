FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8000

WORKDIR /app

# install dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# copy app
COPY . /app

EXPOSE ${PORT}

# use environment DB path so host can mount a volume at /data
ENV DATABASE_PATH=/data/foods.db

CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:8000", "app:app"]
