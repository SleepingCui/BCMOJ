import os
import json
import socket
import shutil
from datetime import datetime
from flask import current_app as app, session
from werkzeug.utils import secure_filename
from app.core.db import db, Problem, Example, JudgeResult, CheckpointResult
from app.core.config import get_config

config = get_config()

SERVER_HOST = config['judge_config']['judge_host']
SERVER_PORT = config['judge_config']['judge_port']
ENABLE_SECURITY_CHECK = config['judge_config']['enable_code_security_check']
USERDATA_PATH = config['app_settings']['userdata_folder']

def submit_solution(problem_id, cpp_file):
    if not cpp_file:
        return {'error': 'No file uploaded'}, 400

    filename = secure_filename(cpp_file.filename)
    temp_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    cpp_file.save(temp_path)

    try:
        problem = Problem.query.filter_by(problem_id=problem_id).first()
        if not problem:
            return {'error': 'Problem not found'}, 404

        examples = Example.query.filter_by(problem_id=problem_id).order_by(Example.example_id).all()

        checkpoints = {
            f"{idx}_in": ex.input for idx, ex in enumerate(examples, 1)
        }
        checkpoints.update({
            f"{idx}_out": ex.output for idx, ex in enumerate(examples, 1)
        })

        config = {
            "timeLimit": problem.time_limit,
            "checkpoints": checkpoints,
            "securityCheck": ENABLE_SECURITY_CHECK
        }
        json_data = json.dumps(config)
        app.logger.info(f"Problem Data : {json_data}")

        results = []
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(30)
            sock.connect((SERVER_HOST, SERVER_PORT))
            sock.sendall(len(filename).to_bytes(4, 'big'))
            sock.sendall(filename.encode('utf-8'))
            filesize = os.path.getsize(temp_path)
            sock.sendall(filesize.to_bytes(8, 'big'))

            with open(temp_path, 'rb') as f:
                for chunk in iter(lambda: f.read(4096), b''):
                    sock.sendall(chunk)

            json_bytes = json_data.encode('utf-8')
            sock.sendall(len(json_bytes).to_bytes(4, 'big'))
            sock.sendall(json_bytes)

            while True:
                length_bytes = sock.recv(4)
                if not length_bytes or int.from_bytes(length_bytes, 'big') == 0:
                    break
                length = int.from_bytes(length_bytes, 'big')
                received = bytearray()
                while len(received) < length:
                    part = sock.recv(length - len(received))
                    if not part:
                        break
                    received.extend(part)

                data = json.loads(received.decode('utf-8'))
                app.logger.info(f"Received data : {data}")

                for key in data:
                    if key.endswith('_res'):
                        idx = key.split('_')[0]
                        results.append({
                            'checkpoint': idx,
                            'result': data.get(f"{idx}_res", 5),
                            'time': data.get(f"{idx}_time", 0.0)
                        })

                user_id = session.get('user_id')
                submit_time = datetime.now()

                judge_result = JudgeResult(userid=user_id, problemid=problem_id, time=submit_time, filepath='')
                db.session.add(judge_result)
                db.session.flush()

                target_dir = os.path.join(USERDATA_PATH, str(user_id), "upload_problem_answers", str(problem_id), str(judge_result.result_id))
                os.makedirs(target_dir, exist_ok=True)
                cpp_target_path = os.path.join(target_dir, "answer.cpp")
                shutil.copy(temp_path, cpp_target_path)

                judge_result.filepath = cpp_target_path

                for result in results:
                    db.session.add(CheckpointResult(
                        result_id=judge_result.result_id,
                        checkpoint_id=int(result['checkpoint']),
                        result=result['result'],
                        time=result['time']
                    ))

                db.session.commit()
                app.logger.info("Transaction committed successfully.")

        return {'status': 'ok', 'results': results}, 200

    except Exception as e:
        app.logger.error(f"Error in submit_solution: {e}")
        db.session.rollback()
        return {'error': str(e)}, 500

    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)
