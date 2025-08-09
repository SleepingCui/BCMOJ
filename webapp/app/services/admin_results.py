from app.core.db import db, JudgeResult, Problem, User
from flask import render_template, session, request, current_app as app
from sqlalchemy import or_


def get_admin_results(page=1, search=''):
    current_user_group = session.get('usergroup')
    if current_user_group not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    results_per_page = 20
    offset = (page - 1) * results_per_page
    query = (db.session.query(JudgeResult.result_id,JudgeResult.userid,User.username,JudgeResult.problemid,Problem.title,JudgeResult.time).join(Problem, JudgeResult.problemid == Problem.problem_id).join(User, JudgeResult.userid == User.userid))
    if search:
        search_pattern = f'%{search}%'
        query = query.filter(
            or_(JudgeResult.result_id.like(search_pattern), JudgeResult.problemid.like(search_pattern), Problem.title.like(search_pattern)))
    total_results = query.count()
    total_pages = (total_results + results_per_page - 1) // results_per_page
    results = (query.order_by(JudgeResult.time.desc()).limit(results_per_page).offset(offset).all())
    app.logger.info(f"Total Results : {total_results}")
    return render_template('admin_result_list.html',results=results,page=page,total_pages=total_pages,search=search)
