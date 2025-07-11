import functools
from flask import session, redirect, url_for, request, jsonify, current_app as app
from functools import wraps

def admin_required(f):
    @functools.wraps(f)
    def decorated_function(*args, **kwargs):
        if "usergroup" not in session or session["usergroup"] != "admin":
            return redirect(url_for("login"))
        return f(*args, **kwargs)
    return decorated_function

def teacher_required(f):
    @functools.wraps(f)
    def decorated_function(*args, **kwargs):
        if "usergroup" not in session or session["usergroup"] not in ["teacher", "admin"]:
            return redirect(url_for("login"))
        return f(*args, **kwargs)
    return decorated_function

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' in session:
            app.logger.info(f"[LOGIN CHECK] User logged in: {session.get('username')}")
        else:
            app.logger.info("[LOGIN CHECK] User not logged in")

        if 'user_id' not in session:
            if request.is_json or request.headers.get('X-Requested-With') == 'XMLHttpRequest':
                return jsonify({'error': 'unauthorized', 'message': 'Please login first'}), 401
            else:
                return redirect(url_for('login', next=request.url))

        return f(*args, **kwargs)
    return decorated_function