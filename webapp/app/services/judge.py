import hashlib
import os
import json
import socket
import shutil
import uuid
from datetime import datetime
from pathlib import Path
from flask import current_app as app, session, request
from app.core.db import db, Problem, Example, JudgeResult, CheckpointResult
from app.core.config import get_config

config = get_config()

SERVER_HOST = config['judge_config']['judge_host']
SERVER_PORT = config['judge_config']['judge_port']
ENABLE_SECURITY_CHECK = config['judge_config']['enable_code_security_check']
USERDATA_PATH = Path(config['app_settings']['userdata_folder'])


def calculate_file_sha256(file_path):
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()


def submit_solution(problem_id, cpp_file):
    if not cpp_file:
        return {'error': 'No file uploaded'}, 400

    filename = f"{uuid.uuid4().hex}.cpp"
    app.logger.info(f"File : {filename}")
    upload_folder = Path(app.config['UPLOAD_FOLDER'])
    upload_folder.mkdir(parents=True, exist_ok=True)
    temp_path = upload_folder / filename
    cpp_file.save(str(temp_path))

    try:
        problem = Problem.query.filter_by(problem_id=problem_id).first()
        if not problem:
            return {'error': 'Problem not found'}, 404

        examples = Example.query.filter_by(problem_id=problem_id).order_by(Example.example_id).all()
        checkpoints = {}
        for idx, ex in enumerate(examples, 1):
            checkpoints[f"{idx}_in"] = ex.input
            checkpoints[f"{idx}_out"] = ex.output

        enable_o2 = request.values.get("enableO2", "false").lower() == "true"

        raw_compare_mode = request.values.get("compare_mode")
        app.logger.info(f"raw value: {raw_compare_mode!r}")
        if raw_compare_mode is None:
            compare_mode = problem.compare_mode
            if compare_mode not in (1, 2, 3, 4):
                compare_mode = 1
        else:
            try:
                compare_mode = int(raw_compare_mode)
                if compare_mode not in (1, 2, 3, 4):
                    compare_mode = 1
            except Exception as e:
                compare_mode = 1
        app.logger.info(f"compare_mode: {compare_mode}")

        config_data = {
            "timeLimit": problem.time_limit,
            "checkpoints": checkpoints,
            "securityCheck": ENABLE_SECURITY_CHECK,
            "enableO2": enable_o2,
            "compareMode": compare_mode
        }

        json_data = json.dumps(config_data)
        app.logger.info(f"Problem Data : {json_data}")
        file_hash = calculate_file_sha256(temp_path)
        app.logger.info(f"Calculated file hash: {file_hash}")

        results = []
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(30)
            sock.connect((SERVER_HOST, SERVER_PORT))
            sock.sendall(len(filename.encode('utf-8')).to_bytes(4, 'big'))
            sock.sendall(filename.encode('utf-8'))
            filesize = temp_path.stat().st_size
            sock.sendall(filesize.to_bytes(8, 'big'))
            with temp_path.open('rb') as f:
                for chunk in iter(lambda: f.read(4096), b''):
                    sock.sendall(chunk)
            json_bytes = json_data.encode('utf-8')
            sock.sendall(len(json_bytes).to_bytes(4, 'big'))
            sock.sendall(json_bytes)

            if file_hash:
                hash_bytes = file_hash.encode('utf-8')
                sock.sendall(len(hash_bytes).to_bytes(4, 'big'))
                sock.sendall(hash_bytes)
            else:
                sock.sendall((0).to_bytes(4, 'big'))
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
        if not user_id:
            return {'error': 'User not logged in'}, 401

        submit_time = datetime.now()
        judge_result = JudgeResult(userid=user_id, problemid=problem_id, time=submit_time, filepath='')
        db.session.add(judge_result)
        db.session.flush()
        target_dir = USERDATA_PATH / str(user_id) / "upload_problem_answers" / str(problem_id) / str(judge_result.result_id)
        target_dir.mkdir(parents=True, exist_ok=True)
        cpp_target_path = target_dir / "answer.cpp"
        shutil.copy(str(temp_path), str(cpp_target_path))

        judge_result.filepath = str(cpp_target_path)
        for result in results:
            db.session.add(CheckpointResult(result_id=judge_result.result_id,checkpoint_id=int(result['checkpoint']),result=result['result'],time=result['time']))
        db.session.commit()
        app.logger.info("Transaction committed successfully.")
        return {'status': 'ok', 'results': results}, 200

    except Exception as e:
        app.logger.error(f"Error in submit_solution: {e}")
        db.session.rollback()
        return {'error': str(e)}, 500
    finally:
        try:
            if temp_path.exists():
                temp_path.unlink()
        except Exception as e:
            app.logger.warning(f"Failed to delete temp file: {e}")
