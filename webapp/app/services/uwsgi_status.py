import time
import requests
from flask import jsonify, current_app as app
from app.core.config import get_config

config = get_config()
UWSGI_STATS_URL = config['app_settings']['uwsgi_stats_url']
MAX_POINTS = 30

history = {
    'requests': [],
    'workers': [],
    'timestamps': []
}

def fetch_uwsgi_stats():
    try:
        resp = requests.get(UWSGI_STATS_URL, timeout=2)
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        app.logger.error(f"Failed to fetch uWSGI stats: {e}")
        return None

def get_uwsgi_stats_data():
    stats = fetch_uwsgi_stats()
    if not stats:
        return jsonify({'error': 'Unable to get uWSGI Status'}), 500

    now = time.time()
    requests_count = stats.get('requests', 0)
    workers = len(stats.get('workers', []))

    history['timestamps'].append(now)
    history['requests'].append(requests_count)
    history['workers'].append(workers)

    if len(history['timestamps']) > MAX_POINTS:
        history['timestamps'].pop(0)
        history['requests'].pop(0)
        history['workers'].pop(0)

    return jsonify({
        'overview': {
            'total_requests': requests_count,
            'worker_count': workers,
            'running': stats.get('running', 0),
            'idle': stats.get('idle', 0),
            'signals': stats.get('signals', 0),
            'listen_queue': stats.get('listen_queue', 0),
            'max_listen_queue': stats.get('max_listen_queue', 0),
        },
        'workers': stats.get('workers', []),
        'history': {
            'timestamps': history['timestamps'],
            'requests': history['requests'],
            'workers': history['workers'],
        }
    })
