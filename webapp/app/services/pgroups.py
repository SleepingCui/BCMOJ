from app.core.db import ProblemGroup, Problem

def get_groups():
    all_groups = ProblemGroup.query.all()
    groups_data = []
    for group in all_groups:
        problems_in_group = Problem.query.filter_by(group_id=group.group_id).order_by(Problem.problem_id.asc()).all()
        group_info = {
            'group_id': group.group_id,
            'group_name': group.group_name,
            'description': group.description or '暂无描述',
            'problems': [
                {'problem_id': p.problem_id, 'title': p.title}
                for p in problems_in_group
            ]
        }
        groups_data.append(group_info)
    return groups_data
