from app.core.db import Problem, Example

def get_problem_with_examples(problem_id, example_limit=2):
    problem = Problem.query.filter_by(problem_id=problem_id).first()
    if not problem:
        return None, []

    examples = Example.query.filter_by(problem_id=problem_id)\
                .order_by(Example.example_id).limit(example_limit).all()

    return problem, examples
