FROM python:3.11-slim

WORKDIR /app/webapp
RUN apt-get update && apt-get install -y --no-install-recommends build-essential default-mysql-client netcat-openbsd && apt-get clean && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt gunicorn waitress

COPY . .
EXPOSE 5000
ENTRYPOINT ["./entrypoint.sh"]
