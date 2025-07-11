from flask import session
from app.core.db import db, Problem, User
from app.core import version

def get_problems_list(query: str, page: int, per_page: int = 20):
    q = Problem.query
    if query:
        like_pattern = f"%{query}%"
        q = q.filter((Problem.title.like(like_pattern)) | (Problem.problem_id.cast(db.String).like(like_pattern)))

    pagination = q.paginate(page=page, per_page=per_page, error_out=False)
    problems = pagination.items
    total_pages = pagination.pages

    user_id = session.get('user_id')
    usergroup = None
    if user_id:
        user = User.query.filter_by(userid=user_id).first()
        if user:
            usergroup = user.usergroup

    username = session.get('username') or "None"
    user_id = user_id or "None"

    return {
        'problems': problems,
        'username': username,
        'user_id': user_id,
        'query': query,
        'page': page,
        'total_pages': total_pages,
        'usergroup': usergroup,
        'version': version
    }
