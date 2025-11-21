import os
from flask import jsonify, request, current_app as app
from app.core.db import db, User, Problem, Example

def get_admin_data():
    with open("config.yml", encoding='utf-8') as f:
        config_yml = f.read()
    
    users = User.query.with_entities(User.userid,User.username,User.email,User.passwd,User.usergroup).all()
    users = [dict(zip(['userid', 'username', 'email', 'passwd', 'usergroup'], user)) for user in users]
    
    return {"config_yml": config_yml,"users": users}

def save_config_yml(content):
    with open("config.yml", "w", encoding="utf-8") as f:
        f.write(content)
    return "OK"

def update_user(data):
    user = User.query.get(data["userid"])
    if user:
        user.username = data["username"]
        user.passwd = data["passwd"]
        user.email = data["email"]
        user.usergroup = data["usergroup"]
        db.session.commit()
        app.logger.info("User information has been updated!")
    return "OK"

def delete_user(userid):
    user = User.query.get(userid)
    if user:
        db.session.delete(user)
        db.session.commit()
    return "OK"