from flask import Flask, request, session, render_template, redirect, url_for, send_from_directory, flash, abort, jsonify

import os

from .core.config import get_config
from .core.logger import setup_logging, log_route_context
from .core.db import DB_URI, init_db
from .services import *

app = Flask(__name__)
setup_logging(app)

#config
config = get_config()
app.secret_key = config['app_settings']['secret_key']
app.config['UPLOAD_FOLDER'] = config['app_settings']['upload_folder']
app.config['USERDATA_FOLDER'] = config['app_settings']['userdata_folder']
app.config['SQLALCHEMY_DATABASE_URI'] = DB_URI
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

#db
init_db(app)

@app.before_request
def set_log_route_name():
    try:
        if request.path == '/uwsgi_stats/data':
            log_route_context.set('nolog')
        else:
            log_route_context.set(request.path)
    except RuntimeError:
        log_route_context.set('unknown')


os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.makedirs(app.config['USERDATA_FOLDER'], exist_ok=True)

def is_logged_in():
    return 'user_id' in session

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('problems'))


@app.route('/')
def index():
    return redirect(url_for('problems'))

@app.route('/favicon.ico')
def favicon():
    return send_from_directory(os.path.join(app.root_path, 'static'), 'favicon.ico', mimetype='image/vnd.microsoft.icon')


@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        success, message = register_user(request.form)
        flash(message, 'success' if success else 'error')
        if success:
            return redirect(url_for('login'))
        else:
            return redirect(url_for('register'))
    return render_template('register.html')


@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username_or_email = request.form.get('username_or_email')
        password = request.form.get('password')
        user = verify_user_login(username_or_email, password)
        if user:
            login_user_session(user, session)
            return redirect(get_redirect_for_user(user, request, url_for))
        else:
            flash('Invalid username/email or password', 'error')

    return render_template('login.html')

@app.route('/forgotpasswd', methods=['GET', 'POST'])
def forgot_password():
    if request.method == 'POST':
        if 'email' in request.form:
            email = request.form.get('email')
            success, msg = start_password_reset(email)
            flash(msg, 'success' if success else 'error')
            if success:
                return render_template('forgot_password.html', step=2)
            else:
                return redirect(url_for('forgot_password'))

        elif 'verification_code' in request.form:
            user_code = request.form.get('verification_code')
            new_password = request.form.get('new_password')
            confirm_password = request.form.get('confirm_password')
            success, msg = verify_and_reset_password(user_code, new_password, confirm_password)
            flash(msg, 'success' if success else 'error')
            if success:
                return redirect(url_for('login'))
            else:
                return render_template('forgot_password.html', step=2)

    return render_template('forgot_password.html', step=1)

@app.route('/problems')
def problems():
    query = request.args.get('q', '').strip()
    page = request.args.get('page', 1, type=int)
    context = get_problems_list(query, page)
    return render_template('problems.html', **context)

@app.route('/problem/<int:problem_id>')
def problem(problem_id):
    if not is_logged_in():
        if request.headers.get('X-Requested-With') == 'XMLHttpRequest':
            return jsonify({'error': 'unauthorized'}), 401
        else:
            return redirect(url_for('login', next=request.url))

    problem, examples = get_problem_with_examples(problem_id)
    if not problem:
        return "题目不存在", 404

    return render_template('problem.html', problem=problem, examples=examples)


@app.route('/submit/<int:problem_id>', methods=['POST'])
@login_required
def submit(problem_id):
    cpp_file = request.files.get('code')
    result, status_code = submit_solution(problem_id, cpp_file)
    return jsonify(result), status_code

@app.route('/results/<int:userid>/', defaults={'resultid': None, 'page': 1})
@app.route('/results/<int:userid>/<int:resultid>', defaults={'page': 1})
@app.route('/results/<int:userid>/page/<int:page>', defaults={'resultid': None})
@login_required
def results(userid, resultid, page):
    if not check_user_authorization(userid):
        return "Unauthorized access", 403
    if resultid is None:
        results, total_pages = get_results_list(userid, page)
        return render_template('result_list.html', results=results, userid=userid, page=page, total_pages=total_pages)
    else:
        judge_result, checkpoint_results, highlighted_code, style_defs = get_result_detail(userid, resultid)
        if not judge_result:
            return "评测结果不存在", 404
        return render_template('result_detail.html', judge_result=judge_result, checkpoint_results=checkpoint_results, highlighted_code=highlighted_code, style_defs=style_defs, userid=userid)
    
# admin
@app.route('/admin')
def admin_page():
    if 'usergroup' not in session or session['usergroup'] != 'admin':
        abort(403)
    return render_template("admin.html")

@app.route("/admin/api")
@admin_required
def admin_api():
    data = get_admin_data()
    return jsonify(data)

@app.route("/admin/api/save_config_yml", methods=["POST"])
@admin_required
def admin_save_config_yml():
    content = request.json.get("content", "")
    return save_config_yml(content)

@app.route("/admin/api/update_user", methods=["POST"])
@admin_required
def admin_update_user():
    data = request.json
    return update_user(data)

@app.route("/admin/api/delete_user", methods=["POST"])
@admin_required
def admin_delete_user():
    userid = request.json.get("userid")
    return delete_user(userid)

@app.route("/admin/api/create_problem", methods=["POST"])
@admin_required
def admin_create_problem():
    data = request.json
    return create_problem(data)

@app.route("/admin/api/update_problem", methods=["POST"])
@admin_required
def admin_update_problem():
    data = request.json
    return update_problem(data)

@app.route("/admin/api/delete_problem", methods=["POST"])
@admin_required
def admin_delete_problem():
    problem_id = request.json.get("problem_id")
    return delete_problem(problem_id)

# teacher
@app.route('/teacher')
def teacher_page():
    if 'usergroup' not in session or session['usergroup'] not in ['admin', 'teacher']:
        abort(403)
    return render_template("teacher.html")

@app.route("/teacher/api")
@teacher_required
def teacher_api():
    return get_teacher_problem_data()

@app.route("/teacher/api/create_problem", methods=["POST"])
@teacher_required
def teacher_create_problem_route():
    return teacher_create_problem(request.json)

@app.route("/teacher/api/update_problem", methods=["POST"])
@teacher_required
def teacher_update_problem_route():
    return teacher_update_problem(request.json)

@app.route("/teacher/api/delete_problem", methods=["POST"])
@teacher_required
def teacher_delete_problem_route():
    return teacher_delete_problem(request.json.get("problem_id"))


# admin results
@app.route('/admin_results', methods=['GET'])
@app.route('/admin_results/page/<int:page>', methods=['GET'])
@login_required
def admin_results(page=1):
    search = request.args.get('search', '', type=str)
    return get_admin_results(page, search)

# about
@app.route("/about")
def about():
    return render_template("about.html", repo="SleepingCui/BCMOJ")

@app.route("/api/contributors")
def get_contributors():
    return fetch_contributors()

#uwsgi STATUS
@app.route('/uwsgi_stats')
def uwsgi_stats():
    return render_template('uwsgi_stats.html')

@app.route('/uwsgi_stats/data')
def uwsgi_stats_data():
    return get_uwsgi_stats_data()

@app.route('/check_update')
@login_required
def check_update():
    return check_update_service()
