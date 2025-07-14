from flask import session, abort
from app.core.db import UserExamResult, User, ExamPaper
from sqlalchemy import or_
from sqlalchemy.orm import joinedload

RESULTS_PER_PAGE = 20

def check_user_authorization(userid):
    current_user_id = session.get('user_id')
    current_user_group = session.get('usergroup')
    if userid != current_user_id and current_user_group not in ['admin', 'teacher']:
        return False
    return True

def get_exam_results_list(userid, page):
    offset = (page - 1) * RESULTS_PER_PAGE
    query = UserExamResult.query.options(
        joinedload(UserExamResult.exam)
    ).filter_by(userid=userid).order_by(UserExamResult.submit_time.desc())

    total_results = query.count()
    total_pages = (total_results + RESULTS_PER_PAGE - 1) // RESULTS_PER_PAGE

    results = query.offset(offset).limit(RESULTS_PER_PAGE).all()
    return results, total_pages

def get_exam_admin_results(page, search):
    current_user_group = session.get('usergroup')
    if current_user_group not in ['admin', 'teacher']:
        abort(403)

    offset = (page - 1) * RESULTS_PER_PAGE

    query = UserExamResult.query.options(
        joinedload(UserExamResult.user),
        joinedload(UserExamResult.exam)
    )

    if search:
        like_search = f"%{search}%"
        query = query.join(User).join(ExamPaper).filter(
            or_(
                UserExamResult.result_id.like(like_search),
                UserExamResult.exam_id.like(like_search),
                ExamPaper.title.ilike(like_search),
                User.username.ilike(like_search)
            )
        )

    total_results = query.count()
    total_pages = (total_results + RESULTS_PER_PAGE - 1) // RESULTS_PER_PAGE

    results = query.order_by(UserExamResult.submit_time.desc()).offset(offset).limit(RESULTS_PER_PAGE).all()
    return results, total_pages
