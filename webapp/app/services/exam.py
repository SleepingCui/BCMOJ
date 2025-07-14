from flask import session
from sqlalchemy.orm import joinedload
from app.core.db import db, ExamPaper, User

def get_exam_context(exam_id: int):
    exam = ExamPaper.query.options(joinedload(ExamPaper.questions)).filter_by(exam_id=exam_id).first_or_404()

    username = session.get('username') or "None"
    user_id = session.get('user_id') or "None"
    usergroup = None

    if user_id != "None":
        user = db.session.get(User, user_id)
        if user:
            usergroup = user.usergroup

    return {
        'exam': exam,
        'username': username,
        'user_id': user_id,
        'usergroup': usergroup
    }