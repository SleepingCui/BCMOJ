from app.core.db import db, Problem, Example
from flask import jsonify, request

def get_teacher_problem_data():
    problems = []
    for p in Problem.query.all():
        examples = Example.query.filter_by(problem_id=p.problem_id).with_entities(Example.input, Example.output).all()
        examples = [{'input': ex.input, 'output': ex.output} for ex in examples]
        problem_data = {"problem_id": p.problem_id,"title": p.title,"description": p.description,"time_limit": p.time_limit,"example_visible_count": p.example_visible_count,"examples": examples}
        problems.append(problem_data)
    return jsonify({"problems": problems})


def teacher_create_problem(data):
    problem = Problem(title=data["title"],description=data["description"], time_limit=data["time_limit"],example_visible_count=data.get("example_visible_count", 2))
    db.session.add(problem)
    db.session.flush()
    for ex in data["examples"]:
        example = Example(problem_id=problem.problem_id,input=ex["input"],output=ex["output"])
        db.session.add(example)
    db.session.commit()
    return "OK"

def teacher_update_problem(data):
    problem = Problem.query.get(data["problem_id"])
    if problem:
        problem.title = data["title"]
        problem.description = data["description"]
        problem.time_limit = data["time_limit"]
        problem.example_visible_count = data.get("example_visible_count", 2)
        Example.query.filter_by(problem_id=problem.problem_id).delete()
        for ex in data["examples"]:
            example = Example(problem_id=problem.problem_id,input=ex["input"],output=ex["output"])
            db.session.add(example)
        db.session.commit()
    return "OK"


def teacher_delete_problem(problem_id):
    problem = Problem.query.get(problem_id)
    if problem:
        db.session.delete(problem)
        db.session.commit()
    return "OK"
