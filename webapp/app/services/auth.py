import hashlib
from flask import session, request
from urllib.parse import urlparse,urljoin
from app.core.db import db, User
from flask import current_app as app

def is_safe_url(target):
    ref_url = urlparse(request.host_url)
    test_url = urlparse(urljoin(request.host_url, target))
    return test_url.scheme in ('http', 'https') and ref_url.netloc == test_url.netloc

def verify_user_login(username_or_email, password):
    hashed_password_sha256 = hashlib.sha256(password.encode()).hexdigest()
    hashed_password_sha1 = hashlib.sha1(password.encode()).hexdigest()

    user = User.query.filter((User.username == username_or_email) | (User.email == username_or_email),User.passwd == hashed_password_sha256).first()
    if not user:
        user = User.query.filter((User.username == username_or_email) | (User.email == username_or_email),User.passwd == hashed_password_sha1).first()
        if user:
            user.passwd = hashed_password_sha256
            db.session.commit()
            app.logger.info(f"Upgraded password hash to SHA256 for user: {user.username}")

    return user

def login_user_session(user, session_obj):
    session_obj.clear()
    session_obj['user_id'] = user.userid
    session_obj['username'] = user.username
    session_obj['usergroup'] = user.usergroup

def get_redirect_for_user(user, request_obj, url_for_func):
    if user.usergroup == 'admin':
        next_page = request_obj.args.get('next')
        if next_page and is_safe_url(next_page):
            return next_page
        return url_for_func('problems')
    else:
        return url_for_func('problems')
