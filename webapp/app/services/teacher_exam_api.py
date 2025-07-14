from flask import jsonify, request, abort, session
from app.core.db import db, ExamPaper, ExamQuestion


def get_teacher_exam_data():
    exams = []
    for e in ExamPaper.query.all():
        questions = []
        for q in e.questions:
            questions.append({
                "question_id": q.question_id,
                "question_text": q.question_text,
                "is_multiple": q.is_multiple,
                "options": q.options,
                "correct_answer": q.correct_answer
            })
        exams.append({
            "exam_id": e.exam_id,
            "title": e.title,
            "time_limit": e.time_limit,
            "questions": questions
        })
    return jsonify({"exams": exams})

def create_exam(data):
    exam = ExamPaper(
        title=data["title"],
        time_limit=data.get("time_limit", 0)
    )
    db.session.add(exam)
    db.session.flush()

    for q in data.get("questions", []):
        question = ExamQuestion(
            exam_id=exam.exam_id,
            question_text=q["question_text"],
            is_multiple=q.get("is_multiple", False),
            options=q.get("options", {}),
            correct_answer=q.get("correct_answer", [])
        )
        db.session.add(question)

    db.session.commit()
    return "OK"

def update_exam(data):
    exam = ExamPaper.query.get(data["exam_id"])
    if not exam:
        abort(404)
    exam.title = data["title"]
    exam.time_limit = data.get("time_limit", 0)

    ExamQuestion.query.filter_by(exam_id=exam.exam_id).delete()

    for q in data.get("questions", []):
        question = ExamQuestion(
            exam_id=exam.exam_id,
            question_text=q["question_text"],
            is_multiple=q.get("is_multiple", False),
            options=q.get("options", {}),
            correct_answer=q.get("correct_answer", [])
        )
        db.session.add(question)

    db.session.commit()
    return "OK"

def delete_exam(exam_id):
    exam = ExamPaper.query.get(exam_id)
    if exam:
        db.session.delete(exam)
        db.session.commit()
    return "OK"
