from flask import Flask, render_template, request, redirect, url_for, jsonify, session, flash, send_file, abort, send_from_directory
from urllib.parse import urlparse, urljoin
import mysql.connector
import os
import socket
import json
import hashlib
import random
import smtplib
import shutil
import functools
import subprocess
import requests
from datetime import datetime
from email.mime.text import MIMEText
from werkzeug.utils import secure_filename
from functools import wraps
from pygments import highlight
from pygments.lexers import CppLexer
from pygments.formatters import HtmlFormatter
import config 

config = config.get_config()
print(f"CONFIG: {config}")

app = Flask(__name__)
app.secret_key = config['SECRET_KEY']
app.config['UPLOAD_FOLDER'] = config['UPLOAD_FOLDER']
app.config['USERDATA_FOLDER'] = config['USERDATA_FOLDER']
app.secret_key = 'your_secret_key_here'
app_port = config['APP_CONFIG']['app_port']
app_host = config['APP_CONFIG']['app_host']

EMAIL_CONFIG = config['EMAIL_CONFIG']
DB_CONFIG = config['DB_CONFIG']
SERVER_HOST = config['JUDGE_CONFIG']['host']
SERVER_PORT = config['JUDGE_CONFIG']['port']
ENABLE_SECURITY_CHECK = config['JUDGE_CONFIG']['enableCodeSecurityCheck']
USERDATA_PATH = config['USERDATA_FOLDER']
CONFIG_YML_PATH = './config.yml'
CONFIG_PROPERTIES_PATH = './config.properties'


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

def get_db_connection():
    return mysql.connector.connect(**DB_CONFIG)

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' in session:
            print(f"[LOGIN CHECK] User logged in: {session.get('username')}")
        else:
            print("[LOGIN CHECK] User not logged in")

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
        print(f"Error sending email: {e}")
        return False


@app.route('/favicon.ico')
def favicon():
    return send_from_directory(os.path.join(app.root_path, 'static'),
                               'favicon.ico', mimetype='image/vnd.microsoft.icon')

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username_or_email = request.form.get('username_or_email')
        password = request.form.get('password')
        hashed_password = hashlib.sha1(password.encode()).hexdigest()

        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute('''
            SELECT * FROM users 
            WHERE (username = %s OR email = %s) AND passwd = %s
        ''', (username_or_email, username_or_email, hashed_password))
        
        user = cursor.fetchone()
        cursor.close()
        conn.close()

        if user:
            session.clear()
            session['user_id'] = user['userid']
            session['username'] = user['username']
            session['usergroup'] = user['usergroup']

           
            if user['usergroup'] == 'admin':
                next_page = request.args.get('next')
                if next_page and is_safe_url(next_page):
                    return redirect(next_page)
                return redirect(url_for('problems')) 
            else:
                
                return redirect(url_for('problems'))

        else:
            flash('Invalid username/email or password', 'error')

    return render_template('login.html')


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

        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute('SELECT * FROM users WHERE username = %s OR email = %s', (username, email))
        existing_user = cursor.fetchone()
        
        if existing_user:
            cursor.close()
            conn.close()
            flash('Username or email already exists', 'error')
            return redirect(url_for('register'))
        
        hashed_password = hashlib.sha1(password.encode()).hexdigest()

        cursor.execute('''
            INSERT INTO users (username, email, passwd, avatar, usergroup)
            VALUES (%s, %s, %s, %s, %s)
        ''', (username, email, hashed_password, '', 'user'))
        user_id = cursor.lastrowid

        cursor.execute('UPDATE users SET avatar = %s WHERE userid = %s', (0, user_id))

        conn.commit()
        cursor.close()
        conn.close()

        flash('Registration successful! Please login.', 'success')
        return redirect(url_for('login'))

    return render_template('register.html')

@app.route('/forgotpasswd', methods=['GET', 'POST'])
def forgot_password():
    if request.method == 'POST':
        if 'email' in request.form:
    
            email = request.form.get('email')
            
            conn = get_db_connection()
            cursor = conn.cursor(dictionary=True)
            cursor.execute('SELECT * FROM users WHERE email = %s', (email,))
            user = cursor.fetchone()
            cursor.close()
            conn.close()
            
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
        
            hashed_password = hashlib.sha1(new_password.encode()).hexdigest()
            email = session['verification_email']
            
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute('UPDATE users SET passwd = %s WHERE email = %s', (hashed_password, email))
            conn.commit()
            cursor.close()
            conn.close()
            
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
    offset = (page - 1) * per_page

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    count_sql = 'SELECT COUNT(*) as total FROM problems'
    select_sql = 'SELECT * FROM problems'
    where_clause = ''
    params = []

    if query:
        where_clause = ' WHERE title LIKE %s OR problem_id LIKE %s'
        params = [f'%{query}%', f'%{query}%']

    cursor.execute(count_sql + where_clause, params)
    total = cursor.fetchone()['total']
    total_pages = (total + per_page - 1) // per_page

    cursor.execute(select_sql + where_clause + ' LIMIT %s OFFSET %s', params + [per_page, offset])
    problems = cursor.fetchall()

    user_id = session.get('user_id')
    usergroup = None
    if user_id:
        cursor.execute('SELECT usergroup FROM users WHERE userid = %s', (user_id,))
        user_data = cursor.fetchone()
        if user_data:
            usergroup = user_data['usergroup']

    cursor.close()
    conn.close()

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
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    cursor.execute('SELECT * FROM problems WHERE problem_id = %s', (problem_id,))
    problem = cursor.fetchone()

    cursor.execute('SELECT * FROM examples WHERE problem_id = %s ORDER BY example_id LIMIT 2', (problem_id,))
    examples = cursor.fetchall()

    cursor.close()
    conn.close()

    if not problem:
        return "题目不存在", 404

    return render_template('problem.html', problem=problem, examples=examples)

@app.route('/submit/<int:problem_id>', methods=['POST'])
@login_required
def submit(problem_id):
    print(f"[LOGIN CHECK] User {'logged in' if is_logged_in() else 'not logged in'}")
    
    cpp_file = request.files.get('code')
    if not cpp_file:
        return jsonify({'error': 'No file uploaded'}), 400

    filename = secure_filename(cpp_file.filename)
    temp_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    cpp_file.save(temp_path)

    conn = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute('SELECT * FROM problems WHERE problem_id = %s', (problem_id,))
        problem = cursor.fetchone()
        cursor.execute('SELECT * FROM examples WHERE problem_id = %s ORDER BY example_id', (problem_id,))
        examples = cursor.fetchall()

        checkpoints = {}
        for idx, example in enumerate(examples, 1):
            checkpoints[f"{idx}_in"] = example['input']
            checkpoints[f"{idx}_out"] = example['output']
        config = {
            "timeLimit": problem['time_limit'],
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
                    print(f'USERID {user_id}')
                
                    submit_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')


                    cursor.execute("""
                        INSERT INTO judge_results (userid, problemid, time, filepath) 
                        VALUES (%s, %s, %s, %s)
                    """, (user_id, problem_id, submit_time, ''))
                    judge_result_id = cursor.lastrowid
                    print(f"Inserted judge result with ID: {judge_result_id}")
                    target_dir = os.path.join(
                        USERDATA_PATH, str(user_id), "upload_problem_answers", 
                        str(problem_id), str(judge_result_id)
                    )
                    os.makedirs(target_dir, exist_ok=True)
                    cpp_target_path = os.path.join(target_dir, "answer.cpp")
                    shutil.copy(temp_path, cpp_target_path)

                    cursor.execute("""
                        UPDATE judge_results SET filepath = %s WHERE result_id = %s
                    """, (cpp_target_path, judge_result_id))
                    for result in results:
                        cursor.execute("""
                            INSERT INTO checkpoint_results (result_id, checkpoint_id, result, time) 
                            VALUES (%s, %s, %s, %s)
                        """, (judge_result_id, result['checkpoint'], result['result'], result['time']))
                    print("Inserted checkpoint results.")
                    conn.commit()
                    print("Transaction committed successfully.")

                except json.JSONDecodeError as e:
                    print(f"Error decoding JSON: {e}")
                    conn.rollback()
                    return jsonify({'error': 'Invalid JSON response'}), 500
                except Exception as e:
                    print(f"Error during database insertion: {e}")
                    conn.rollback()
                    return jsonify({'error': str(e)}), 500

        return jsonify({
            'status': 'ok',
            'results': results
        })

    except Exception as e:
        print(f"Error in submit function: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)
        if conn:
            conn.close()


@app.route('/results/<int:userid>/', defaults={'resultid': None, 'page': 1})
@app.route('/results/<int:userid>/<int:resultid>', defaults={'page': 1})
@app.route('/results/<int:userid>/page/<int:page>', defaults={'resultid': None})
@login_required
def results(userid, resultid, page):
    current_user_id = session.get('user_id')
    current_user_group = session.get('usergroup') 
    if userid != current_user_id and current_user_group not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    results_per_page = 20
    offset = (page - 1) * results_per_page

    if resultid is None:
        cursor.execute('SELECT COUNT(*) FROM judge_results WHERE userid = %s', (userid,))
        total_results = cursor.fetchone()['COUNT(*)']
        total_pages = (total_results + results_per_page - 1) // results_per_page
        cursor.execute('SELECT * FROM judge_results WHERE userid = %s ORDER BY time DESC LIMIT %s OFFSET %s', (userid, results_per_page, offset))
        results = cursor.fetchall()

        cursor.close()
        conn.close()

        return render_template('result_list.html', results=results, userid=userid, page=page, total_pages=total_pages)
    else:
        cursor.execute('SELECT * FROM judge_results WHERE result_id = %s AND userid = %s', (resultid, userid))
        judge_result = cursor.fetchone()

        if not judge_result:
            return "评测结果不存在", 404

        cursor.execute('SELECT * FROM checkpoint_results WHERE result_id = %s ORDER BY checkpoint_id', (resultid,))
        checkpoint_results = cursor.fetchall()

        cpp_code = ""
        if judge_result['filepath'] and os.path.exists(judge_result['filepath']):
            with open(judge_result['filepath'], 'r', encoding='utf-8', errors='ignore') as f:
                cpp_code = f.read()

        formatter = HtmlFormatter(style="friendly", linenos=True, full=False, cssclass="codehilite")
        highlighted_code = highlight(cpp_code, CppLexer(), formatter)
        style_defs = formatter.get_style_defs('.codehilite')

        cursor.close()
        conn.close()

        return render_template('result_detail.html',
                               judge_result=judge_result,
                               checkpoint_results=checkpoint_results,
                               highlighted_code=highlighted_code,
                               style_defs=style_defs,
                               userid=userid)

#admin
@app.route('/admin')
def admin_page():
    if 'usergroup' not in session or session['usergroup'] != 'admin':
        abort(403) 
    
    return send_file("templates/admin.html")
@app.route("/admin/api")
@admin_required
def admin_api():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    with open("config.yml", encoding='utf-8') as f:
        config_yml = f.read()
    with open("config.properties", encoding='utf-8') as f:
        config_properties = f.read()

    cursor.execute("SELECT userid, username, email, passwd, 'user' AS usergroup FROM users")
    users = cursor.fetchall()
    cursor.execute("SELECT * FROM problems")
    problems_raw = cursor.fetchall()
    problems = []
    for p in problems_raw:
        cursor.execute("SELECT input, output FROM examples WHERE problem_id = %s", (p["problem_id"],))
        examples = cursor.fetchall()
        p["examples"] = examples
        problems.append(p)

    cursor.close()
    conn.close()

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

    subprocess.Popen(["pkill", "-f", "flask"])
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
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE users SET username=%s, passwd=%s, email=%s, usergroup=%s WHERE userid=%s
    """, (data["username"], data["passwd"], data["email"], data["usergroup"], data["userid"]))
    conn.commit()
    cursor.close()
    conn.close()
    print("用户信息已更新！")
    return "OK"

@app.route("/admin/api/delete_user", methods=["POST"])
@admin_required
def delete_user():
    userid = request.json.get("userid")
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM users WHERE userid=%s", (userid,))
    conn.commit()
    cursor.close()
    conn.close()
    return "OK"

@app.route("/admin/api/create_problem", methods=["POST"])
@admin_required
def create_problem():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("INSERT INTO problems (title, description, time_limit) VALUES (%s, %s, %s)",
                   (data["title"], data["description"], data["time_limit"]))
    problem_id = cursor.lastrowid
    for ex in data["examples"]:
        cursor.execute("INSERT INTO examples (problem_id, input, output) VALUES (%s, %s, %s)",
                       (problem_id, ex["input"], ex["output"]))
    conn.commit()
    cursor.close()
    conn.close()
    return "OK"

@app.route("/admin/api/update_problem", methods=["POST"])
@admin_required
def update_problem():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE problems SET title=%s, description=%s, time_limit=%s WHERE problem_id=%s",
                   (data["title"], data["description"], data["time_limit"], data["problem_id"]))
    cursor.execute("DELETE FROM examples WHERE problem_id=%s", (data["problem_id"],))
    for ex in data["examples"]:
        cursor.execute("INSERT INTO examples (problem_id, input, output) VALUES (%s, %s, %s)",
                       (data["problem_id"], ex["input"], ex["output"]))
    conn.commit()
    cursor.close()
    conn.close()
    return "OK"

@app.route("/admin/api/delete_problem", methods=["POST"])
@admin_required
def delete_problem():
    problem_id = request.json.get("problem_id")
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM problems WHERE problem_id=%s", (problem_id,))
    conn.commit()
    cursor.close()
    conn.close()
    return "OK"



#teacher
@app.route('/teacher/teacher_api', methods=['GET'])
def get_teacher_data():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute('SELECT * FROM problems')
    problems = cursor.fetchall()
    
    result = []
    for problem in problems:
        problem_id = problem['problem_id']
        
        cursor.execute('SELECT * FROM examples WHERE problem_id = %s', (problem_id,))
        examples = cursor.fetchall()
        
        result.append({
            'problem_id': problem_id,
            'title': problem['title'],
            'description': problem['description'],
            'time_limit': problem['time_limit'],
            'examples': [{'input': ex['input'], 'output': ex['output']} for ex in examples]
        })

    cursor.close()
    conn.close()
    
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

    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute('INSERT INTO problems (title, description, time_limit) VALUES (%s, %s, %s)', 
                   (title, description, time_limit))
    conn.commit()

    problem_id = cursor.lastrowid
    for ex in examples:
        cursor.execute('INSERT INTO examples (input, output, problem_id) VALUES (%s, %s, %s)', 
                       (ex['input'], ex['output'], problem_id))

    conn.commit()
    cursor.close()
    conn.close()

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

    conn = get_db_connection()
    cursor = conn.cursor()


    cursor.execute('UPDATE problems SET title = %s, description = %s, time_limit = %s WHERE problem_id = %s',
                   (title, description, time_limit, problem_id))
    conn.commit()
    cursor.execute('DELETE FROM examples WHERE problem_id = %s', (problem_id,))
    conn.commit()

    for ex in examples:
        cursor.execute('INSERT INTO examples (input, output, problem_id) VALUES (%s, %s, %s)', 
                       (ex['input'], ex['output'], problem_id))
    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({'message': '问题更新成功'}), 200

@app.route('/teacher/api/teacher_delete_problem', methods=['POST'])
def teacher_delete_problem():
    data = request.get_json()
    problem_id = data.get('problem_id')

    if not problem_id:
        return jsonify({'error': 'Missing problem_id'}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('DELETE FROM examples WHERE problem_id = %s', (problem_id,))
    cursor.execute('DELETE FROM problems WHERE problem_id = %s', (problem_id,))
    conn.commit()

    cursor.close()
    conn.close()

    return jsonify({'message': '问题已删除'}), 200
@app.route('/teacher/problem_manage', methods=['GET', 'POST'])
@login_required
def teacher_problem_manage():
    usergroup = session.get('usergroup')
    if usergroup not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM problems")
    problems = cursor.fetchall()
    if request.method == 'POST':
        if 'create_problem' in request.form:
            title = request.form['title']
            description = request.form['description']
            time_limit = request.form['time_limit']

            cursor.execute("INSERT INTO problems (title, description, time_limit) VALUES (%s, %s, %s)",
                           (title, description, time_limit))
            problem_id = cursor.lastrowid

            examples = request.form.getlist('examples')
            for example in examples:
                input_data, output_data = example.split('|')
                cursor.execute("INSERT INTO examples (problem_id, input, output) VALUES (%s, %s, %s)",
                               (problem_id, input_data, output_data))

            conn.commit()
            flash("题目创建成功", "success")

    cursor.close()
    conn.close()

    return render_template('teacher_problem_manage.html', problems=problems)


#admin results
@app.route('/admin_results', defaults={'page': 1, 'search': ''}, methods=['GET'])
@app.route('/admin_results/page/<int:page>', methods=['GET'])
@app.route('/admin_results/search/<search>', defaults={'page': 1}, methods=['GET'])
@login_required
def admin_results(page, search):
    current_user_group = session.get('usergroup')
    if current_user_group not in ['admin', 'teacher']:
        return "Unauthorized access", 403

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    results_per_page = 20
    offset = (page - 1) * results_per_page

    query = 'SELECT * FROM judge_results WHERE 1=1'
    params = []
    if search:
        query += ' AND (result_id LIKE %s OR problemid LIKE %s OR title LIKE %s)'
        search_pattern = f'%{search}%'
        params = [search_pattern, search_pattern, search_pattern]

    cursor.execute(query, params)
    cursor.fetchall()
    total_results = cursor.rowcount
    total_pages = (total_results + results_per_page - 1) // results_per_page

    query += f' ORDER BY time DESC LIMIT %s OFFSET %s'
    params.extend([results_per_page, offset])
    cursor.execute(query, params)
    results = cursor.fetchall()

    cursor.close()
    conn.close()

    return render_template('admin_result_list.html',
                           results=results,
                           page=page,
                           total_pages=total_pages,
                           search=search)
    
#about
GITHUB_REPO = "SleepingCui/BCMOJ"

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
        return jsonify(contributors)
    else:
        return jsonify([]), response.status_code
#run

if __name__ == '__main__':
    app.run(port=app_port,host=app_host)