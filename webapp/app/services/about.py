import requests
from flask import jsonify, current_app as app

def fetch_contributors():
    url = f"https://api.github.com/repos/SleepingCui/BCMOJ/contributors"
    try:
        response = requests.get(url)
        if response.status_code == 200:
            data = response.json()
            contributors = [
                {
                    "login": user["login"],
                    "avatar_url": user["avatar_url"],
                    "html_url": user["html_url"]
                }
                for user in data
            ]
            app.logger.info(f"Contributors fetched: {len(contributors)}")
            return jsonify(contributors)
        else:
            app.logger.warning(f"Failed to fetch contributors: {response.status_code}")
            return jsonify([]), response.status_code
    except Exception as e:
        app.logger.error(f"Error fetching contributors: {e}")
        return jsonify([]), 500
