from flask import Flask, render_template, request, redirect, url_for, jsonify
import mysql.connector
import os
import socket
import json
from werkzeug.utils import secure_filename

app = Flask(__name__)
UPLOAD_FOLDER = 'tmp'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'password',
    'database': 'coding_problems'
}

SERVER_HOST = 'localhost'
SERVER_PORT = 5000

def get_db_connection():
    return mysql.connector.connect(**DB_CONFIG)

@app.route('/')
def index():
    return redirect(url_for('problems'))

@app.route('/problems')
def problems():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute('SELECT * FROM problems')
    problems = cursor.fetchall()
    cursor.close()
    conn.close()
    return render_template('problems.html', problems=problems)

@app.route('/problem/<int:problem_id>')
def problem(problem_id):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # 获取题目
    cursor.execute('SELECT * FROM problems WHERE problem_id = %s', (problem_id,))
    problem = cursor.fetchone()

    # 获取前两个示例
    cursor.execute('SELECT * FROM examples WHERE problem_id = %s ORDER BY example_id LIMIT 2', (problem_id,))
    examples = cursor.fetchall()

    cursor.close()
    conn.close()

    if not problem:
        return "题目不存在", 404

    return render_template('problem.html', problem=problem, examples=examples)

@app.route('/submit/<int:problem_id>', methods=['POST'])
def submit(problem_id):
    cpp_file = request.files.get('code')
    if not cpp_file:
        return jsonify({'error': 'No file uploaded'}), 400

    filename = secure_filename(cpp_file.filename)
    temp_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    cpp_file.save(temp_path)

    try:
        # 从数据库取题目信息
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)

        cursor.execute('SELECT * FROM problems WHERE problem_id = %s', (problem_id,))
        problem = cursor.fetchone()

        cursor.execute('SELECT * FROM examples WHERE problem_id = %s ORDER BY example_id', (problem_id,))
        examples = cursor.fetchall()

        cursor.close()
        conn.close()

        # 生成配置
        checkpoints = {}
        for idx, example in enumerate(examples, 1):
            checkpoints[f"{idx}_in"] = example['input']
            checkpoints[f"{idx}_out"] = example['output']

        config = {
            "timeLimit": problem['time_limit'],
            "checkpoints": checkpoints,
            "securityCheck": True
        }

        json_data = json.dumps(config, indent=2)

        # 连接评测服务器
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

                # 解析每条响应
                try:
                    data = json.loads(received.decode('utf-8'))
                    for key in data:
                        if key.endswith('_res'):
                            idx = key.split('_')[0]
                            result_code = data.get(f"{idx}_res", "Unknown")
                            time_used = data.get(f"{idx}_time", 0.0)

                            # 结果映射
                            result_mapping = {
                                -5: "Security Check Failed",
                                -4: "Compile Error",
                                -3: "Wrong Answer",
                                2: "Real Time Limit Exceeded",
                                4: "Runtime Error",
                                5: "System Error",
                                1: "Accepted",
                                "default": "Unknown Status"
                            }

                            result_text = result_mapping.get(result_code, result_mapping["default"])

                            results.append({
                                'checkpoint': idx,
                                'result': result_text,
                                'time': f"{time_used:.2f} ms"
                            })
                except json.JSONDecodeError:
                    # 不是有效的JSON，直接存原文
                    results.append({
                        'checkpoint': 'Unknown',
                        'result': 'Invalid JSON response',
                        'time': 'N/A'
                    })

        return jsonify({
            'status': 'ok',
            'results': results
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


if __name__ == '__main__':
    app.run(debug=True,port=4949)
