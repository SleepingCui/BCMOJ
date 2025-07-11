import os
from flask import session, render_template
from app.core.db import JudgeResult, CheckpointResult
from pygments import highlight
from pygments.lexers import CppLexer
from pygments.formatters import HtmlFormatter

def check_user_authorization(userid):
    current_user_id = session.get('user_id')
    current_user_group = session.get('usergroup')
    if userid != current_user_id and current_user_group not in ['admin', 'teacher']:
        return False
    return True

def get_results_list(userid, page, results_per_page=20):
    offset = (page - 1) * results_per_page
    total_results = JudgeResult.query.filter_by(userid=userid).count()
    total_pages = (total_results + results_per_page - 1) // results_per_page
    results = (JudgeResult.query.filter_by(userid=userid)
               .order_by(JudgeResult.time.desc())
               .limit(results_per_page)
               .offset(offset)
               .all())
    return results, total_pages

def get_result_detail(userid, resultid):
    judge_result = JudgeResult.query.filter_by(result_id=resultid, userid=userid).first()
    if not judge_result:
        return None, None, None, None

    checkpoint_results = (CheckpointResult.query
                          .filter_by(result_id=resultid)
                          .order_by(CheckpointResult.checkpoint_id)
                          .all())

    cpp_code = ""
    if judge_result.filepath and os.path.exists(judge_result.filepath):
        with open(judge_result.filepath, 'r', encoding='utf-8', errors='ignore') as f:
            cpp_code = f.read()

    formatter = HtmlFormatter(style="friendly", linenos=True, full=False, cssclass="codehilite")
    highlighted_code = highlight(cpp_code, CppLexer(), formatter)
    style_defs = formatter.get_style_defs('.codehilite')

    return judge_result, checkpoint_results, highlighted_code, style_defs
