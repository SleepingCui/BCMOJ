from flask import session, jsonify
from app.core import version
import requests

GITHUB_REPO = "SleepingCui/BCMOJ"

def get_latest_release():
    url = f"https://api.github.com/repos/{GITHUB_REPO}/releases/latest"
    response = requests.get(url, timeout=5)
    if response.status_code == 200:
        return response.json().get("tag_name")
    return None

def check_update_service():
    if session.get('usergroup') != 'admin':
        return jsonify({"error": "Unauthorized"}), 403

    latest = get_latest_release()
    if not latest:
        return jsonify({"error": "无法获取最新版本"}), 500

    if latest == version:
        return jsonify({"message": f"已是最新版本：{version}"}), 200
    else:
        return jsonify({
            "message": f"发现新版本：{latest}（当前为 {version}）",
            "latest": latest
        }), 200
