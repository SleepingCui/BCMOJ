#!/bin/bash
set -e

CONFIG_FILE="config.yml"

check_env_vars() {
    echo "[entrypoint] Checking config.yml for unescaped env variables..."
    if grep -q '\${.*}' "$CONFIG_FILE"; then
        echo "[entrypoint][ERROR] Found unescaped environment variables in $CONFIG_FILE"
        grep '\${.*}' "$CONFIG_FILE"
        exit 1
    fi
    echo "[entrypoint] All environment variables in config.yml are properly expanded."
}

DB_HOST=${DB_HOST:-mysql}
DB_PORT=${DB_PORT:-3306}
MAX_RETRIES=30
RETRY_INTERVAL=2
i=0

echo "[entrypoint] Waiting for MySQL at $DB_HOST:$DB_PORT..."
until nc -z "$DB_HOST" "$DB_PORT"; do
    i=$((i+1))
    if [ $i -ge $MAX_RETRIES ]; then
        echo "[entrypoint] MySQL did not become ready in time. Exiting."
        exit 1
    fi
    sleep $RETRY_INTERVAL
done
echo "[entrypoint] MySQL is ready."

if [ ! -f "$CONFIG_FILE" ]; then
    echo "[entrypoint] Creating default config.yml..."
    cat > "$CONFIG_FILE" <<-EOF
db_config:
  db_name: bcmoj
  db_host: ${DB_HOST}
  db_password: ${DB_PASSWORD:-password}
  db_port: ${DB_PORT}
  db_user: root
email_config:
  email_password: ${EMAIL_PASSWORD:-password}
  email_sender: ${EMAIL_SENDER:-example@example.com}
  email_smtp_port: ${EMAIL_SMTP_PORT:-587}
  email_smtp_server: ${EMAIL_SMTP_SERVER:-smtp.example.com} 
judge_config:
  enable_code_security_check: false
  judge_host: ${JUDGE_HOST:-localhost}
  judge_port: ${JUDGE_PORT:-12345}
app_settings:
  secret_key: ${SECRET_KEY:-your_secret_key_here}
  upload_folder: tmp
  userdata_folder: userdata
  disable_color_log: false
EOF
    echo "[entrypoint] config.yml created."
else
    echo "[entrypoint] config.yml already exists, skipping."
fi

check_env_vars
exec python manage.py run --port=5000 --host=0.0.0.0 --wsgi
