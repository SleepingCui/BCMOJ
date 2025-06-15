from flask import Flask, render_template, request, redirect, url_for, jsonify, session, flash, send_file, abort, send_from_directory
from urllib.parse import urlparse, urljoin
from datetime import datetime
from email.mime.text import MIMEText
from werkzeug.utils import secure_filename
from functools import wraps
from pygments import highlight
from pygments.lexers import CppLexer
from pygments.formatters import HtmlFormatter

import os
import socket
import json
import hashlib
import time
import random
import smtplib
import shutil
import functools
import requests

from .config import config
from .logger import setup_logging, log_route_context
from .db import db, DB_URI, init_db
from .db import User, Problem, JudgeResult, CheckpointResult, Example

app = Flask(__name__)
setup_logging(app)

#config
config = config.get_config()
EMAIL_CONFIG = config['email_config']
UWSGI_STATS_URL = config['app_settings']['uwsgi_stats_url']
SERVER_HOST = config['judge_config']['judge_host']
SERVER_PORT = config['judge_config']['judge_port']
ENABLE_SECURITY_CHECK = config['judge_config']['enable_code_security_check']
USERDATA_PATH = config['app_settings']['userdata_folder']
SECRET_KEY = config['app_settings']['secret_key']
CONFIG_YML_PATH = './config.yml'
CONFIG_PROPERTIES_PATH = './config.properties'
GITHUB_REPO = "SleepingCui/BCMOJ"
MAX_POINTS = 30

#app config
app.secret_key = SECRET_KEY
app.config['UPLOAD_FOLDER'] = config['app_settings']['upload_folder']
app.config['USERDATA_FOLDER'] = config['app_settings']['userdata_folder']
app.secret_key = config['app_settings']['secret_key']
app.config['SQLALCHEMY_DATABASE_URI'] = DB_URI
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

#db init
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

def admin_required(f):
    @functools.wraps(f)
    def decorated_function(*args, **kwargs):
        if "usergroup" not in session or session["usergroup"] != "admin":
            return redirect(url_for("login"))
        return f(*args, **kwargs)

    return decorated_function

def is_safe_url(target):
    ref_url = urlparse(request.host_url)
    test_url = urlparse(urljoin(request.host_url, target))
    return test_url.scheme in ('http', 'https') and ref_url.netloc == test_url.netloc

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' in session:
            app.logger.info(f"[LOGIN CHECK] User logged in: {session.get('username')}")
        else:
            app.logger.info("[LOGIN CHECK] User not logged in")

        if 'user_id' not in session:
            if request.is_json or request.headers.get('X-Requested-With') == 'XMLHttpRequest':
                return jsonify({'error': 'unauthorized', 'message': 'Please login first'}), 401
            else:
                return redirect(url_for('login', next=request.url))

        return f(*args, **kwargs)

    return decorated_function

def send_verification_email(email, verification_code):
    try:
        msg = MIMEText(f'Your verification code is: {verification_code}')
        msg['Subject'] = 'Password Recovery Verification Code'
        msg['From'] = EMAIL_CONFIG['sender']
        msg['To'] = email

        with smtplib.SMTP(EMAIL_CONFIG['smtp_server'], EMAIL_CONFIG['smtp_port']) as server:
            server.starttls()
            server.login(EMAIL_CONFIG['sender'], EMAIL_CONFIG['password'])
            server.send_message(msg)
        return True
    except Exception as e:
        app.logger.error(f"Error sending email: {e}")
        return False

@app.route('/favicon.ico')
def favicon():
    return send_from_directory(os.path.join(app.root_path, 'static'),
                               'favicon.ico', mimetype='image/vnd.microsoft.icon')


@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        username = request.form.get('username')
        email = request.form.get('email')
        password = request.form.get('password')
        confirm_password = request.form.get('confirm_password')

        if password != confirm_password:
            flash('Passwords do not match', 'error')
            return redirect(url_for('register'))

        existing_user = User.query.filter((User.username == username) | (User.email == email)).first()

        if existing_user:
            flash('Username or email already exists', 'error')
            return redirect(url_for('register'))

        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        new_user = User(username=username, email=email, passwd=hashed_password, avatar='0', usergroup='user')
        db.session.add(new_user)
        db.session.commit()

        flash('Registration successful! Please login.', 'success')
        return redirect(url_for('login'))

    return render_template('register.html')


@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username_or_email = request.form.get('username_or_email')
        password = request.form.get('password')

        hashed_password_sha256 = hashlib.sha256(password.encode()).hexdigest()
        hashed_password_sha1 = hashlib.sha1(password.encode()).hexdigest()

        user = User.query.filter(
            (User.username == username_or_email) | (User.email == username_or_email),
            User.passwd == hashed_password_sha256
        ).first()

        if not user:
            user = User.query.filter(
                (User.username == username_or_email) | (User.email == username_or_email),
                User.passwd == hashed_password_sha1
            ).first()
            if user:
                user.passwd = hashed_password_sha256
                db.session.commit()
                app.logger.info(f"Upgraded password hash to SHA256 for user: {user.username}")

        if user:
            session.clear()
            session['user_id'] = user.userid
            session['username'] = user.username
            session['usergroup'] = user.usergroup

            if user.usergroup == 'admin':
                next_page = request.args.get('next')
                if next_page and is_safe_url(next_page):
                    return redirect(next_page)
                return redirect(url_for('problems'))
            else:
                return redirect(url_for('problems'))
        else:
            flash('Invalid username/email or password', 'error')

    return render_template('login.html')


@app.route('/forgotpasswd', methods=['GET', 'POST'])
def forgot_password():
    if request.method == 'POST':
        if 'email' in request.form:
            email = request.form.get('email')
            user = User.query.filter_by(email=email).first()

            if not user:
                flash('Email not found', 'error')
                return redirect(url_for('forgot_password'))

            verification_code = ''.join([str(random.randint(0, 9)) for _ in range(6)])
            session['verification_code'] = verification_code
            session['verification_email'] = email

            if send_verification_email(email, verification_code):
                flash('Verification code sent to your email', 'success')
                return render_template('forgot_password.html', step=2)
            else:
                flash('Failed to send verification code. Please try again.', 'error')
                return redirect(url_for('forgot_password'))

        elif 'verification_code' in request.form:
            user_code = request.form.get('verification_code')
            new_password = request.form.get('new_password')
            confirm_password = request.form.get('confirm_password')

            if new_password != confirm_password:
                flash('Passwords do not match', 'error')
                return render_template('forgot_password.html', step=2)

            if 'verification_code' not in session or 'verification_email' not in session:
                flash('Session expired. Please start again.', 'error')
                return redirect(url_for('forgot_password'))

            if user_code != session['verification_code']:
                flash('Invalid verification code', 'error')
                return render_template('forgot_password.html', step=2)

            hashed_password = hashlib.sha256(new_password.encode()).hexdigest()
            email = session['verification_email']

            user = User.query.filter_by(email=email).first()
            if user:
                user.passwd = hashed_password
                db.session.commit()

            session.pop('verification_code', None)
            session.pop('verification_email', None)

            flash('Password updated successfully. Please login.', 'success')
            return redirect(url_for('login'))

    return render_template('forgot_password.html', step=1)



@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('problems'))


@app.route('/')
def index():
    return redirect(url_for('problems'))


@app.route('/problems')
def problems():
    query = request.args.get('q', '').strip()
    page = request.args.get('page', 1, type=int)
    per_page = 20

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

    return render_template('problems.html', problems=problems, username=username, user_id=user_id,
                           query=query, page=page, total_pages=total_pages, usergroup=usergroup)

@app.route('/problem/<int:problem_id>')
def problem(problem_id):
    if not is_logged_in():
        if request.headers.get('X-Requested-With') == 'XMLHttpRequest':
            return jsonify({'error': 'unauthorized'}), 401
        else:
            return redirect(url_for('login', next=request.url))

    problem = Problem.query.filter_by(problem_id=problem_id).first()
    if not problem:
        return "题目不存在", 404

    examples = Example.query.filter_by(problem_id=problem_id).order_by(Example.example_id).limit(2).all()

    return render_template('problem.html', problem=problem, examples=examples)



@app.route('/submit/<int:problem_id>', methods=['POST'])
@login_required
def submit(problem_id):
    app.logger.info(f"[LOGIN CHECK] User {'logged in' if is_logged_in() else 'not logged in'}")

    cpp_file = request.files.get('code')
    if not cpp_file:
        return jsonify({'error': 'No file uploaded'}), 400

    filename = secure_filename(cpp_file.filename)
    temp_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    cpp_file.save(temp_path)

    try:
        problem = Problem.query.filter_by(problem_id=problem_id).first()
        if not problem:
            return jsonify({'error': 'Problem not found'}), 404
        examples = Example.query.filter_by(problem_id=problem_id).order_by(Example.example_id).all()

        checkpoints = {}
        for idx, example in enumerate(examples, 1):
            checkpoints[f"{idx}_in"] = example.input
            checkpoints[f"{idx}_out"] = example.output
        config = {
            "timeLimit": problem.time_limit,
            "checkpoints": checkpoints,
            "securityCheck": ENABLE_SECURITY_CHECK
        }
        json_data = json.dumps(config, indent=2)

        results = []
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(30)
            sock.connect((SERVER_HOST, SERVER_PORT))
            sock.sendall(len(filename).to_bytes(4, 'big'))
            sock.sendall(filename.encode('utf-8'))
            filesize = os.path.getsize(temp_path)
            sock.sendall(filesize.to_bytes(8, 'big'))

            with open(temp_path, 'rb') as f:
                while chunk := f.read(4096):
                    sock.sendall(chunk)

            json_bytes = json_data.encode('utf-8')
            sock.sendall(len(json_bytes).to_bytes(4, 'big'))
            sock.sendall(json_bytes)

            while True:
                length_bytes = sock.recv(4)
                if not length_bytes:
                    break
                length = int.from_bytes(length_bytes, 'big')
                if length == 0:
                    break
                received = bytearray()
                while len(received) < length:
                    part = sock.recv(min(4096, length - len(received)))
                    if not part:
                        break
                    received.extend(part)

                try:
                    data = json.loads(received.decode('utf-8'))
                    for key in data:
                        if key.endswith('_res'):
                            idx = key.split('_')[0]
                            result_code = data.get(f"{idx}_res", 5)
                            time_used = data.get(f"{idx}_time", 0.0)
                            results.append({
                                'checkpoint': idx,
                                'result': result_code,
                                'time': time_used
                            })

                    user_id = session.get('user_id')
                    app.logger.info(f'USERID {user_id}')
                    submit_time = datetime.now()

                    judge_result = JudgeResult(userid=user_id, problemid=problem_id, time=submit_time, filepath='')
                    db.session.add(judge_result)
                    db.session.flush()

                    target_dir = os.path.join(
                        USERDATA_PATH, str(user_id), "upload_problem_answers",
                        str(problem_id), str(judge_result.result_id)
                    )
                    os.makedirs(target_dir, exist_ok=True)
                    cpp_target_path = os.path.join(target_dir, "answer.cpp")
                    shutil.copy(temp_path, cpp_target_path)

                    judge_result.filepath = cpp_target_path

                    for result in results:
                        checkpoint_result = CheckpointResult(
                            result_id=judge_result.result_id,
                            checkpoint_id=int(result['checkpoint']),
                            result=result['result'],
                            time=result['time']
                        )
                        db.session.add(checkpoint_result)

                    db.session.commit()
                    app.logger.info("Transaction committed successfully.")

                except json.JSONDecodeError as e:
                    app.logger.error(f"Error decoding JSON: {e}")
                    db.session.rollback()
                    return jsonify({'error': 'Invalid JSON response'}), 500
                except Exception as e:
                    app.logger.error(f"Error during database insertion: {e}")
                    db.session.rollback()
                    return jsonify({'error': str(e)}), 500

        return jsonify({
            'status': 'ok',
            'results': results
        })

    except Exception as e:
        app.logger.error(f"Error in submit function: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)

@app.route('/results/<int:userid>/', defaults={'resultid': None, 'page': 1})
@app.route('/results/<int:userid>/<int:resultid>', defaults={'page': 1})
@app.route('/results/<int:userid>/page/<int:page>', defaults={'resultid': None})
@login_required
def results(userid, resultid, page):
    current_user_id = session.get('user_id')
    current_user_group = session.get('usergroup')
    if userid != current_user_id and current_user_group not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    results_per_page = 20
    offset = (page - 1) * results_per_page

    if resultid is None:
        total_results = JudgeResult.query.filter_by(userid=userid).count()
        total_pages = (total_results + results_per_page - 1) // results_per_page
        results = JudgeResult.query.filter_by(userid=userid).order_by(JudgeResult.time.desc()).limit(results_per_page).offset(offset).all()

        return render_template('result_list.html', results=results, userid=userid, page=page, total_pages=total_pages)
    else:
        judge_result = JudgeResult.query.filter_by(result_id=resultid, userid=userid).first()

        if not judge_result:
            return "评测结果不存在", 404

        checkpoint_results = CheckpointResult.query.filter_by(result_id=resultid).order_by(CheckpointResult.checkpoint_id).all()

        cpp_code = ""
        if judge_result.filepath and os.path.exists(judge_result.filepath):
            with open(judge_result.filepath, 'r', encoding='utf-8', errors='ignore') as f:
                cpp_code = f.read()

        formatter = HtmlFormatter(style="friendly", linenos=True, full=False, cssclass="codehilite")
        highlighted_code = highlight(cpp_code, CppLexer(), formatter)
        style_defs = formatter.get_style_defs('.codehilite')

        return render_template('result_detail.html',
                               judge_result=judge_result,
                               checkpoint_results=checkpoint_results,
                               highlighted_code=highlighted_code,
                               style_defs=style_defs,
                               userid=userid)

# admin
@app.route('/admin')
def admin_page():
    if 'usergroup' not in session or session['usergroup'] != 'admin':
        abort(403)

    return send_file("templates/admin.html")


@app.route("/admin/api")
@admin_required
def admin_api():
    with open("config.yml", encoding='utf-8') as f:
        config_yml = f.read()
    with open("config.properties", encoding='utf-8') as f:
        config_properties = f.read()
    users = User.query.with_entities(
        User.userid,
        User.username,
        User.email,
        User.passwd,
        db.literal('user').label('usergroup')
    ).all()
    users = [dict(zip(['userid', 'username', 'email', 'passwd', 'usergroup'], user)) for user in users]

    problems = []
    for p in Problem.query.all():
        examples = Example.query.filter_by(problem_id=p.problem_id).with_entities(Example.input, Example.output).all()
        examples = [{'input': ex.input, 'output': ex.output} for ex in examples]
        problem_data = {
            "problem_id": p.problem_id,
            "title": p.title,
            "description": p.description,
            "time_limit": p.time_limit,
            "examples": examples
        }
        problems.append(problem_data)

    return jsonify({
        "config_yml": config_yml,
        "config_properties": config_properties,
        "users": users,
        "problems": problems
    })

@app.route("/admin/api/save_config_yml", methods=["POST"])
@admin_required
def save_config_yml():
    content = request.json.get("content", "")
    with open("config.yml", "w", encoding="utf-8") as f:
        f.write(content)
    return "OK"


@app.route("/admin/api/save_config_properties", methods=["POST"])
@admin_required
def save_config_properties():
    content = request.json.get("content", "")
    with open("config.properties", "w", encoding="utf-8") as f:
        f.write(content)
    return "OK"


@app.route("/admin/api/update_user", methods=["POST"])
@admin_required
def update_user():
    data = request.json
    user = User.query.get(data["userid"])
    if user:
        user.username = data["username"]
        user.passwd = data["passwd"]
        user.email = data["email"]
        user.usergroup = data["usergroup"]
        db.session.commit()
        app.logger.info("User information has been updated!")
    return "OK"


@app.route("/admin/api/delete_user", methods=["POST"])
@admin_required
def delete_user():
    userid = request.json.get("userid")
    user = User.query.get(userid)
    if user:
        db.session.delete(user)
        db.session.commit()
    return "OK"


@app.route("/admin/api/create_problem", methods=["POST"])
@admin_required
def create_problem():
    data = request.json
    problem = Problem(
        title=data["title"],
        description=data["description"],
        time_limit=data["time_limit"]
    )
    db.session.add(problem)
    db.session.flush()
    
    for ex in data["examples"]:
        example = Example(
            problem_id=problem.problem_id,
            input=ex["input"],
            output=ex["output"]
        )
        db.session.add(example)
    
    db.session.commit()
    return "OK"


@app.route("/admin/api/update_problem", methods=["POST"])
@admin_required
def update_problem():
    data = request.json
    problem = Problem.query.get(data["problem_id"])
    if problem:
        problem.title = data["title"]
        problem.description = data["description"]
        problem.time_limit = data["time_limit"]
        
        Example.query.filter_by(problem_id=problem.problem_id).delete()
        
        for ex in data["examples"]:
            example = Example(
                problem_id=problem.problem_id,
                input=ex["input"],
                output=ex["output"]
            )
            db.session.add(example)
        
        db.session.commit()
    return "OK"


@app.route("/admin/api/delete_problem", methods=["POST"])
@admin_required
def delete_problem():
    problem_id = request.json.get("problem_id")
    problem = Problem.query.get(problem_id)
    if problem:
        db.session.delete(problem)
        db.session.commit()
    return "OK"

# teacher
@app.route('/teacher/teacher_api', methods=['GET'])
def get_teacher_data():
    problems = Problem.query.all()
    result = []
    for problem in problems:
        examples = Example.query.filter_by(problem_id=problem.problem_id).all()
        result.append({
            'problem_id': problem.problem_id,
            'title': problem.title,
            'description': problem.description,
            'time_limit': problem.time_limit,
            'examples': [{'input': ex.input, 'output': ex.output} for ex in examples]
        })

    return jsonify({'problems': result})


@app.route('/teacher/api/teacher_create_problem', methods=['POST'])
def teacher_create_problem():
    data = request.get_json()
    title = data.get('title')
    description = data.get('description')
    time_limit = data.get('time_limit')
    examples = data.get('examples')

    if not title or not description or not time_limit:
        return jsonify({'error': 'Missing required fields'}), 400

    problem = Problem(title=title, description=description, time_limit=time_limit)
    db.session.add(problem)
    db.session.flush()

    for ex in examples:
        example = Example(
            problem_id=problem.problem_id,
            input=ex['input'],
            output=ex['output']
        )
        db.session.add(example)

    db.session.commit()

    return jsonify({'message': '问题创建成功'}), 201


@app.route('/teacher/api/teacher_update_problem', methods=['POST'])
def teacher_update_problem():
    data = request.get_json()
    problem_id = data.get('problem_id')
    title = data.get('title')
    description = data.get('description')
    time_limit = data.get('time_limit')
    examples = data.get('examples')

    if not problem_id or not title or not description or not time_limit:
        return jsonify({'error': 'Missing required fields'}), 400

    problem = Problem.query.get(problem_id)
    if not problem:
        return jsonify({'error': 'Problem not found'}), 404

    problem.title = title
    problem.description = description
    problem.time_limit = time_limit

    Example.query.filter_by(problem_id=problem_id).delete()

    for ex in examples:
        example = Example(
            problem_id=problem_id,
            input=ex['input'],
            output=ex['output']
        )
        db.session.add(example)

    db.session.commit()

    return jsonify({'message': '问题更新成功'}), 200


@app.route('/teacher/api/teacher_delete_problem', methods=['POST'])
def teacher_delete_problem():
    data = request.get_json()
    problem_id = data.get('problem_id')

    if not problem_id:
        return jsonify({'error': 'Missing problem_id'}), 400

    problem = Problem.query.get(problem_id)
    if problem:
        db.session.delete(problem)
        db.session.commit()

    return jsonify({'message': '问题已删除'}), 200


@app.route('/teacher/problem_manage', methods=['GET', 'POST'])
@login_required
def teacher_problem_manage():
    usergroup = session.get('usergroup')
    if usergroup not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    problems = Problem.query.all()
    
    if request.method == 'POST' and 'create_problem' in request.form:
        title = request.form['title']
        description = request.form['description']
        time_limit = request.form['time_limit']

        problem = Problem(title=title, description=description, time_limit=time_limit)
        db.session.add(problem)
        db.session.flush()

        examples = request.form.getlist('examples')
        for example in examples:
            input_data, output_data = example.split('|')
            example = Example(
                problem_id=problem.problem_id,
                input=input_data,
                output=output_data
            )
            db.session.add(example)
            app.logger.info(f"Insert[problemid={problem.problem_id} input_data={input_data} output_data={output_data}]")

        db.session.commit()
        app.logger.info("Success")
        flash("题目创建成功", "success")

    return render_template('teacher_problem_manage.html', problems=problems)

# admin results
@app.route('/admin_results', defaults={'page': 1, 'search': ''}, methods=['GET'])
@app.route('/admin_results/page/<int:page>', methods=['GET'])
@app.route('/admin_results/search/<search>', defaults={'page': 1}, methods=['GET'])
@login_required
def admin_results(page, search):
    current_user_group = session.get('usergroup')
    if current_user_group not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    results_per_page = 20
    offset = (page - 1) * results_per_page

    query = JudgeResult.query.join(Problem, JudgeResult.problemid == Problem.problem_id)
    
    if search:
        search_pattern = f'%{search}%'
        query = query.filter(
            db.or_(
                JudgeResult.result_id.like(search_pattern),
                JudgeResult.problemid.like(search_pattern),
                Problem.title.like(search_pattern)
            )
        )

    total_results = query.count()
    total_pages = (total_results + results_per_page - 1) // results_per_page

    results = query.order_by(JudgeResult.time.desc())\
                  .limit(results_per_page)\
                  .offset(offset)\
                  .all()

    app.logger.info(f"Results={results} Page={page}/{total_pages}")
    return render_template('admin_result_list.html',
                         results=results,
                         page=page,
                         total_pages=total_pages,
                         search=search)
# about

@app.route("/about")
def about():
    return render_template("about.html", repo=GITHUB_REPO)


@app.route("/api/contributors")
def get_contributors():
    url = f"https://api.github.com/repos/{GITHUB_REPO}/contributors"
    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        contributors = [
            {
                "login": user["login"],
                "avatar_url": user["avatar_url"],
                "html_url": user["html_url"]
            }
            for user in data
        ]
        app.logger.info(jsonify(contributors))
        return jsonify(contributors)
    else:
        app.logger.info(jsonify([]), response.status_code)
        return jsonify([]), response.status_code


#uwsgi STATUS

history = {
    'requests': [],
    'workers': [],
    'timestamps': []
}
def fetch_uwsgi_stats():
    try:
        resp = requests.get(UWSGI_STATS_URL, timeout=2)
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        return None

@app.route('/uwsgi_stats')
def uwsgi_stats():
    return render_template('uwsgi_stats.html')

@app.route('/uwsgi_stats/data')
def uwsgi_stats_data():
    stats = fetch_uwsgi_stats()
    if not stats:
        app.logger.error(f'Unable to get uWSGI Status : {stats}')
        return jsonify({'error': 'Unable to get uWSGI Status'}), 500

    now = time.time()
    requests_count = stats.get('requests', 0)
    workers = len(stats.get('workers', []))

    history['timestamps'].append(now)
    history['requests'].append(requests_count)
    history['workers'].append(workers)
    if len(history['timestamps']) > MAX_POINTS:
        history['timestamps'].pop(0)
        history['requests'].pop(0)
        history['workers'].pop(0)

    return jsonify({
        'overview': {
            'total_requests': requests_count,
            'worker_count': workers,
            'running': stats.get('running', 0),
            'idle': stats.get('idle', 0),
            'signals': stats.get('signals', 0),
            'listen_queue': stats.get('listen_queue', 0),
            'max_listen_queue': stats.get('max_listen_queue', 0),
        },
        'workers': stats.get('workers', []),
        'history': {
            'timestamps': history['timestamps'],
            'requests': history['requests'],
            'workers': history['workers'],
        }
    })
