from flask import session
from app.core.db import db, ExamPaper, User
from app.core import version

def get_exam_list(query: str, page: int, per_page: int = 20):
    q = ExamPaper.query
    if query:
        like_pattern = f"%{query}%"
        q = q.filter((ExamPaper.title.like(like_pattern)) | (ExamPaper.exam_id.cast(db.String).like(like_pattern)))

    pagination = q.paginate(page=page, per_page=per_page, error_out=False)
    exams = pagination.items
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
        'exams': exams,
        'username': username,
        'user_id': user_id,
        'query': query,
        'page': page,
        'total_pages': total_pages,
        'usergroup': usergroup,
        'version': version
    }
