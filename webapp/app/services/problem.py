from app.core.db import Problem, Example

def get_problem_with_examples(problem_id, example_limit=None):
    problem = Problem.query.filter_by(problem_id=problem_id).first()
    if not problem:
        return None, []
    limit = example_limit if example_limit is not None else problem.example_visible_count
    examples = Example.query.filter_by(problem_id=problem_id).order_by(Example.example_id).limit(limit).all()
    return problem, examples
