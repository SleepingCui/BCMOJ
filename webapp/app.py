from flask import Flask, render_template, request, redirect, url_for, jsonify, session, flash
from urllib.parse import urlparse, urljoin
import mysql.connector
import os
import socket
import json
import hashlib
import random
import smtplib
from email.mime.text import MIMEText
from werkzeug.utils import secure_filename
from functools import wraps

import config 

config = config.get_config()
print(f"CONFIG: {config}")

app = Flask(__name__)
app.secret_key = config['SECRET_KEY']
app.config['UPLOAD_FOLDER'] = config['UPLOAD_FOLDER']
app.config['AVATAR_FOLDER'] = config['AVATAR_FOLDER']
app.secret_key = 'your_secret_key_here'
app_port = config['APP_CONFIG']['app_port']
app_host = config['APP_CONFIG']['app_host']

EMAIL_CONFIG = config['EMAIL_CONFIG']
DB_CONFIG = config['DB_CONFIG']
SERVER_HOST = config['JUDGE_CONFIG']['host']
SERVER_PORT = config['JUDGE_CONFIG']['port']
ENABLE_SECURITY_CHECK = config['JUDGE_CONFIG']['enableCodeSecurityCheck']


os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.makedirs(app.config['AVATAR_FOLDER'], exist_ok=True)

def is_logged_in():
    return 'user_id' in session

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
            next_page = request.args.get('next')
            if next_page and is_safe_url(next_page):
                return redirect(next_page)
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
            INSERT INTO users (username, email, passwd, avatar)
            VALUES (%s, %s, %s, %s)
        ''', (username, email, hashed_password, ''))
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
            # 验证码发送
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
            
            # 更新密码
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

    cursor.close()
    conn.close()

    username = session.get('username') or "None"
    user_id = session.get('user_id') or "None"

    return render_template('problems.html', problems=problems, username=username, user_id=user_id,
                           query=query, page=page, total_pages=total_pages)


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
                            result_mapping = {
                                -5: "Security Check Failed",
                                -4: "Compile Error",
                                -3: "Wrong Answer",
                                2: "Real Time Limit Exceeded",
                                4: "Runtime Error",
                                5: "System Error",
                                1: "Accepted",
                            }

                            results.append({
                                'checkpoint': idx,
                                'result': result_code, 
                                'time': time_used
                            })

                
                    user_id = session.get('user_id')
                    print(f'USERID {user_id}')

                
                    cursor.execute("""
                        INSERT INTO judge_results (userid, problemid) 
                        VALUES (%s, %s)
                    """, (user_id, problem_id))

                    judge_result_id = cursor.lastrowid
                    print(f"Inserted judge result with ID: {judge_result_id}")
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
                    results.append({
                        'checkpoint': 'Unknown',
                        'result': 'Invalid JSON response',
                        'time': 'N/A'
                    })
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


@app.route('/result/<int:result_id>')
@login_required
def result(result_id):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)


    cursor.execute('SELECT * FROM judge_results WHERE result_id = %s', (result_id,))
    judge_result = cursor.fetchone()

    if not judge_result:
        return "结果未找到", 404
    cursor.execute('SELECT * FROM checkpoint_results WHERE result_id = %s ORDER BY checkpoint_id', (result_id,))
    checkpoint_results = cursor.fetchall()
    cursor.close()
    conn.close()

    return render_template('result.html', judge_result=judge_result, checkpoint_results=checkpoint_results)




if __name__ == '__main__':
    app.run(debug=True,port=app_port,host=app_host)