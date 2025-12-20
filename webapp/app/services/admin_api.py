import os
from flask import jsonify, request, current_app as app
from app.core.db import db, User, Problem, Example
from ruamel.yaml import YAML

yaml = YAML()
SENSITIVE_KEYS = {'secret_key'}

def get_admin_data():
    with open("config.yml", encoding='utf-8') as f:
        config_data = yaml.load(f)

    general_config = _mask_sensitive_data(config_data, mask_values=False)
    general_config.pop('db_config', None)
    users = User.query.with_entities(User.userid, User.username, User.email, User.passwd, User.usergroup).all()
    users = [
        dict(zip(['userid', 'username', 'email', 'passwd', 'usergroup'], user))
        for user in users
    ]
    return {"general_config": general_config, "users": users}

def save_general_config(new_general_config):
    with open("config.yml", 'r', encoding='utf-8') as f:
        current_config = yaml.load(f)

    if not isinstance(current_config, dict):
        print("[Warning] Current config.yml does not contain a dictionary. Initializing with an empty one.")
        current_config = {}
    _update_nested_dict(current_config, new_general_config)
    with open("config.yml", 'w', encoding='utf-8') as f:
        yaml.dump(current_config, f)
    return "OK"

def _mask_sensitive_data(data, mask_values=False):
    if isinstance(data, dict):
        new_dict = {}
        for k, v in data.items():
            if k in SENSITIVE_KEYS:
                if mask_values:
                    new_dict[k] = "***"
            else:
                new_dict[k] = _mask_sensitive_data(v, mask_values)
        return new_dict
    elif isinstance(data, list):
        return [_mask_sensitive_data(item, mask_values) for item in data]
    else:
        return data

def _update_nested_dict(original, updates):
    for key, value in updates.items():
        if key in original and isinstance(original[key], dict) and isinstance(value, dict):
            _update_nested_dict(original[key], value)
        else:
            original[key] = value


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