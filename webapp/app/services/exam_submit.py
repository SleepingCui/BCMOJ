# services/exam_submit.py

from app.core.db import db, ExamPaper, UserAnswer, UserExamResult
from datetime import datetime
import json

def handle_exam_submission(exam_id, user_id, form_data):
    exam = ExamPaper.query.options(db.joinedload(ExamPaper.questions)).filter_by(exam_id=exam_id).first()
    if not exam:
        return None, "试卷不存在"

    correct_count = 0

    user_result = UserExamResult(
        userid=user_id,
        exam_id=exam.exam_id,
        submit_time=datetime.utcnow(),
        score=0,  # 先设0，后面更新
    )
    db.session.add(user_result)
    db.session.flush()  # 生成 user_result.result_id

    answers = []

    for question in exam.questions:
        qid = question.question_id
        selected_raw = form_data.getlist(f'q_{qid}')
        selected = sorted(selected_raw)
        correct = sorted(question.correct_answer or [])

        is_correct = (selected == correct)
        if is_correct:
            correct_count += 1

        ua = UserAnswer(
            result_id=user_result.result_id,
            question_id=qid,
            selected=selected,
            is_correct=is_correct
        )
        answers.append(ua)

    # 更新成绩
    if exam.questions:
        user_result.score = round(correct_count / len(exam.questions) * 100, 2)
    else:
        user_result.score = 0

    db.session.add_all(answers)
    db.session.commit()

    return user_result, None
