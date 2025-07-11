import hashlib
from flask import flash, redirect, url_for
from app.core.db import db, User

def register_user(form):
    username = form.get('username')
    email = form.get('email')
    password = form.get('password')
    confirm_password = form.get('confirm_password')

    if password != confirm_password:
        return False, 'Passwords do not match'

    existing_user = User.query.filter((User.username == username) | (User.email == email)).first()
    if existing_user:
        return False, 'Username or email already exists'

    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    new_user = User(username=username, email=email, passwd=hashed_password, avatar='0', usergroup='user')

    db.session.add(new_user)
    db.session.commit()

    return True, 'Registration successful! Please login.'
