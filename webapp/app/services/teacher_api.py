from app.core.db import db, Problem, ProblemGroup, Example
from flask import jsonify, request

def get_teacher_problem_data():
    problems = []
    for p in Problem.query.all():
        examples = Example.query.filter_by(problem_id=p.problem_id).with_entities(Example.input, Example.output).all()
        examples = [{'input': ex.input, 'output': ex.output} for ex in examples]
        problem_data = {
            "problem_id": p.problem_id,
            "title": p.title,
            "description": p.description,
            "time_limit": p.time_limit,
            "mem_limit": p.mem_limit,        
            "example_visible_count": p.example_visible_count,
            "compare_mode": p.compare_mode,
            "examples": examples,
            "group_id": p.group_id  
        }
        problems.append(problem_data)
    return jsonify({"problems": problems})



def teacher_create_problem(data):
    problem = Problem(
        title=data["title"],
        description=data["description"],
        time_limit=data["time_limit"],
        mem_limit=data.get("mem_limit", 1000), 
        example_visible_count=data.get("example_visible_count", 2),
        compare_mode=data.get("compare_mode", 1)
    )
    db.session.add(problem)
    db.session.flush()
    for ex in data["examples"]:
        example = Example(problem_id=problem.problem_id, input=ex["input"], output=ex["output"])
        db.session.add(example)
    db.session.commit()
    return "OK"

def teacher_update_problem(data):
    try:
        problem_id = data.get('problem_id')
        if problem_id:
            problem = Problem.query.get_or_404(problem_id)
            problem.title = data['title']
            problem.description = data['description']
            problem.time_limit = data['time_limit']
            problem.mem_limit = data['mem_limit']
            problem.compare_mode = data['compare_mode']
            problem.example_visible_count = data['example_visible_count']
            problem.group_id = data.get('group_id') 
            
            db.session.commit()
            return jsonify({'message': '题目更新成功'})
        else:
            new_problem = Problem(
                title=data['title'],
                description=data['description'],
                time_limit=data['time_limit'],
                mem_limit=data['mem_limit'],
                compare_mode=data['compare_mode'],
                example_visible_count=data['example_visible_count'],
                group_id=data.get('group_id')
            )
            db.session.add(new_problem)
            for ex in data["examples"]:
                example = Example(problem_id=new_problem.problem_id, input=ex["input"], output=ex["output"])
                db.session.add(example)
            db.session.commit()
            return jsonify({'message': '题目创建成功', 'problem_id': new_problem.problem_id})
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

def teacher_delete_problem(problem_id):
    problem = Problem.query.get(problem_id)
    if problem:
        db.session.delete(problem)
        db.session.commit()
    return "OK"

def teacher_groups_api():
    try:
        groups = ProblemGroup.query.all()
        groups_data = []
        for g in groups:
            problem_count = Problem.query.filter_by(group_id=g.group_id).count()
            groups_data.append({
                'group_id': g.group_id,
                'group_name': g.group_name,
                'description': g.description,
                'problem_count': problem_count
            })
        return jsonify({'groups': groups_data})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

def teacher_create_group_route():
    try:
        data = request.json
        if not data or 'group_name' not in data:
            return jsonify({'error': 'Missing group_name'}), 400

        new_group = ProblemGroup(
            group_name=data['group_name'],
            description=data.get('description', '')
        )
        db.session.add(new_group)
        db.session.commit()
        
        return jsonify({'message': '题组创建成功', 'group_id': new_group.group_id}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500


def teacher_update_group_route():
    try:
        data = request.json
        if not data or 'group_id' not in data or 'group_name' not in data:
            return jsonify({'error': 'Missing group_id or group_name'}), 400

        group = ProblemGroup.query.get_or_404(data['group_id'])
        group.group_name = data['group_name']
        group.description = data.get('description', '')
        db.session.commit()
        return jsonify({'message': '题组更新成功'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500


def teacher_delete_group_route():
    try:
        data = request.json
        if not data or 'group_id' not in data:
            return jsonify({'error': 'Missing group_id'}), 400

        group_id = data['group_id']
        Problem.query.filter_by(group_id=group_id).update({Problem.group_id: None})
        group_to_delete = ProblemGroup.query.get_or_404(group_id)
        db.session.delete(group_to_delete)
        db.session.commit()
        return jsonify({'message': '题组删除成功'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500