import os
from flask import jsonify, request, current_app as app
from app.core.db import db, User, Problem, Example

def get_admin_data():
    with open("config.yml", encoding='utf-8') as f:
        config_yml = f.read()
    
    users = User.query.with_entities(User.userid,User.username,User.email,User.passwd,User.usergroup).all()
    users = [dict(zip(['userid', 'username', 'email', 'passwd', 'usergroup'], user)) for user in users]
    problems = []
    for p in Problem.query.all():
        examples = Example.query.filter_by(problem_id=p.problem_id).with_entities(Example.input, Example.output).all()
        examples = [{'input': ex.input, 'output': ex.output} for ex in examples]
        problem_data = {"problem_id": p.problem_id,"title": p.title,"description": p.description,"time_limit": p.time_limit,"examples": examples}
        problems.append(problem_data)
    return {"config_yml": config_yml,"users": users,"problems": problems}

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

def create_problem(data):
    problem = Problem(title=data["title"],description=data["description"],time_limit=data["time_limit"])
    db.session.add(problem)
    db.session.flush()
    for ex in data["examples"]:
        example = Example(problem_id=problem.problem_id,input=ex["input"],output=ex["output"])
        db.session.add(example)
    db.session.commit()
    return "OK"

def update_problem(data):
    problem = Problem.query.get(data["problem_id"])
    if problem:
        problem.title = data["title"]
        problem.description = data["description"]
        problem.time_limit = data["time_limit"]
        Example.query.filter_by(problem_id=problem.problem_id).delete()
        for ex in data["examples"]:
            example = Example(problem_id=problem.problem_id,input=ex["input"],output=ex["output"])
            db.session.add(example)
        db.session.commit()
    return "OK"

def delete_problem(problem_id):
    problem = Problem.query.get(problem_id)
    if problem:
        db.session.delete(problem)
        db.session.commit()
    return "OK"
