import json
from flask import abort, session
from app.core.db import UserExamResult, ExamPaper, ExamQuestion, UserAnswer
from sqlalchemy.orm import joinedload

def check_user_authorization(userid):
    current_user_id = session.get('user_id')
    current_user_group = session.get('usergroup')
    if userid != current_user_id and current_user_group not in ['admin', 'teacher']:
        return False
    return True

def get_exam_result_data(userid, resultid):
    if not check_user_authorization(userid):
        abort(403)

    user_result = UserExamResult.query.options(
        joinedload(UserExamResult.exam).joinedload(ExamPaper.questions),
        joinedload(UserExamResult.answers)
    ).filter_by(result_id=resultid, userid=userid).first()

    if not user_result:
        abort(404, description="答卷不存在")

    exam = user_result.exam
    user_answers = {ua.question_id: ua for ua in user_result.answers}

    question_results = []
    for q in exam.questions:
        ua = user_answers.get(q.question_id)
        selected = ua.selected if ua else []
        if not isinstance(selected, list):
            selected = json.loads(selected or "[]")

        correct_answer = q.correct_answer if isinstance(q.correct_answer, list) else json.loads(q.correct_answer or "[]")

        question_results.append({
            "question_id": q.question_id,
            "question_text": q.question_text,
            "options": q.options,
            "correct_answer": correct_answer,
            "user_answer": selected,
            "is_correct": ua.is_correct if ua else False,
        })

    return {
        "exam": exam,
        "exam_answer": user_result,
        "question_results": question_results,
        "userid": userid
    }
