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

# Compare Modes
CM_STD = 1  # Standard
CM_IGN_SP = 2  # Ignore Spaces
CM_CI = 3  # Case Insensitive
CM_FP = 4  # Floating Point
VALID_CM = {CM_STD, CM_IGN_SP, CM_CI, CM_FP}

# Judge result codes
R_AC = 1      # Accepted
R_WA = -3     # Wrong Answer
R_TLE = 2     # Time Limit Exceeded
R_MLE = 3     # Memory Limit Exceeded
R_RE = 4      # Runtime Error
R_SE = 5      # System Error
R_CE = -4     # Compile Error
R_SC_FAILED = -5  # Security Check Failed
R_UNKNOWN = 0     # Unknown error code

# JSON keys
K_IN = "_in"
K_OUT = "_out"
K_RES = "_res"
K_TIME = "_time"
K_MEM = "_mem"

class JudgeServerClient:
    """
    Client for communicating with JudgeServer.
    """
    def __init__(self, host, port, timeout=30):
        self.host = host
        self.port = port
        self.timeout = timeout

    def send_code_and_config(self, file_path: Path, config: dict, file_hash: str = ""):
        """
        Send code file and config to JudgeServer, receive results.

        Args:
            file_path (Path): Path to C++ source file.
            config (dict): Judge configuration.
            file_hash (str): Optional file hash.

        Returns:
            list[dict] | None: List of judge results or None on failure.
        """
        results = []
        json_data = json.dumps(config)
        app.logger.info(f"Sending config to JudgeServer: {json_data}")

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(self.timeout)
                app.logger.info(f"Connecting to JudgeServer at {self.host}:{self.port}")
                sock.connect((self.host, self.port))
                app.logger.info("Connected to JudgeServer.")

                # filename
                filename_bytes = file_path.name.encode('utf-8')
                sock.sendall(len(filename_bytes).to_bytes(4, 'big'))
                sock.sendall(filename_bytes)

                # file size and content
                filesize = file_path.stat().st_size
                sock.sendall(filesize.to_bytes(8, 'big'))
                app.logger.info(f"Sending file ({file_path.name}, {filesize} bytes)...")
                with file_path.open('rb') as f:
                    for chunk in iter(lambda: f.read(4096), b''):
                        sock.sendall(chunk)
                app.logger.info("File sent.")

                # config JSON
                json_bytes = json_data.encode('utf-8')
                sock.sendall(len(json_bytes).to_bytes(4, 'big'))
                sock.sendall(json_bytes)
                app.logger.info("Config sent.")

                # file hash
                if file_hash:
                    hash_bytes = file_hash.encode('utf-8')
                    sock.sendall(len(hash_bytes).to_bytes(4, 'big'))
                    sock.sendall(hash_bytes)
                    app.logger.info(f"File hash sent: {file_hash}")
                else:
                    sock.sendall((0).to_bytes(4, 'big'))
                    app.logger.info("No file hash sent.")

                # results
                app.logger.info("Waiting for results from JudgeServer...")
                while True:
                    length_bytes = sock.recv(4)
                    if not length_bytes or int.from_bytes(length_bytes, 'big') == 0:
                        app.logger.info("Received end-of-transmission marker from JudgeServer.")
                        break
                    length = int.from_bytes(length_bytes, 'big')
                    received = bytearray()
                    while len(received) < length:
                        part = sock.recv(length - len(received))
                        if not part:
                            raise ConnectionError("Incomplete data received from JudgeServer")
                        received.extend(part)

                    data = json.loads(received.decode('utf-8'))
                    app.logger.info(f"Received data chunk from JudgeServer: {data}")

                    # parse results
                    for key in data:
                        if key.endswith(K_RES):
                            idx = key.split(K_RES)[0]
                            results.append({
                                'checkpoint': idx,
                                'result': data.get(f"{idx}{K_RES}", R_UNKNOWN),
                                'time': data.get(f"{idx}{K_TIME}", 0.0),
                                'mem': data.get(f"{idx}{K_MEM}", 0)
                            })

        except socket.timeout:
            app.logger.error("Socket connection to JudgeServer timed out.")
            return None
        except ConnectionError as e:
            app.logger.error(f"Connection error with JudgeServer: {e}")
            return None
        except json.JSONDecodeError as e:
            app.logger.error(f"Failed to decode JSON response from JudgeServer: {e}")
            return None
        except Exception as e:
            app.logger.error(f"Unexpected error during JudgeServer communication: {e}")
            return None

        app.logger.info(f"Successfully received results from JudgeServer: {results}")
        return results


def calculate_file_sha256(file_path):
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()


def _save_file_temp(cpp_file):
    filename = f"{uuid.uuid4().hex}.cpp"
    app.logger.info(f"File : {filename}")
    upload_folder = Path(app.config['UPLOAD_FOLDER'])
    upload_folder.mkdir(parents=True, exist_ok=True)
    temp_path = upload_folder / filename
    cpp_file.save(str(temp_path))
    return temp_path


def _get_problem_data(problem_id):
    problem = Problem.query.filter_by(problem_id=problem_id).first()
    if not problem:
        return None, None, 'Problem not found'

    examples = Example.query.filter_by(problem_id=problem_id).order_by(Example.example_id).all()
    checkpoints = {}
    for idx, ex in enumerate(examples, 1):
        checkpoints[f"{idx}{K_IN}"] = ex.input
        checkpoints[f"{idx}{K_OUT}"] = ex.output

    return problem, examples, None


def _parse_options(problem_compare_mode):
    enable_o2 = request.values.get("enableO2", "false").lower() == "true"
    raw_compare_mode = request.values.get("compare_mode")
    app.logger.info(f"raw value: {raw_compare_mode!r}")

    compare_mode = problem_compare_mode
    if raw_compare_mode is not None:
        try:
            compare_mode = int(raw_compare_mode)
            if compare_mode not in VALID_CM:
                compare_mode = CM_STD
        except (ValueError, TypeError):
            compare_mode = CM_STD

    app.logger.info(f"compare_mode: {compare_mode}")
    return enable_o2, compare_mode


def _build_config(problem, examples, enable_o2, compare_mode):
    checkpoints = {}
    for idx, ex in enumerate(examples, 1):
        checkpoints[f"{idx}{K_IN}"] = ex.input
        checkpoints[f"{idx}{K_OUT}"] = ex.output

    config_data = {
        "timeLimit": problem.time_limit,
        "memLimit": problem.mem_limit,
        "checkpoints": checkpoints,
        "securityCheck": ENABLE_SECURITY_CHECK,
        "enableO2": enable_o2,
        "compareMode": compare_mode
    }
    return config_data


def _save_to_db(user_id, problem_id, submit_time, results, source_file_path):
    """Save judge results to database."""
    judge_result = JudgeResult(
        userid=user_id,
        problemid=problem_id,
        time=submit_time,
        filepath=''
    )
    db.session.add(judge_result)
    db.session.flush()

    target_dir = USERDATA_PATH / str(user_id) / "upload_problem_answers" / str(problem_id) / str(judge_result.result_id)
    target_dir.mkdir(parents=True, exist_ok=True)
    cpp_target_path = target_dir / "answer.cpp"
    shutil.copy(str(source_file_path), str(cpp_target_path))
    judge_result.filepath = str(cpp_target_path)

    for result in results:
        db.session.add(CheckpointResult(
            result_id=judge_result.result_id,
            checkpoint_id=int(result['checkpoint']),
            result=result['result'],
            time=result['time'],
            mem=result['mem']
        ))

    db.session.commit()
    app.logger.info("Transaction committed successfully.")
    return judge_result


def _cleanup(temp_path):
    """Delete temporary file."""
    try:
        if temp_path.exists():
            temp_path.unlink()
    except Exception as e:
        app.logger.warning(f"Failed to delete temp file: {e}")


def submit_solution(problem_id, cpp_file):
    if not cpp_file:
        return {'error': 'No file uploaded'}, 400

    temp_path = None
    try:
        temp_path = _save_file_temp(cpp_file)

        problem, examples, error_msg = _get_problem_data(problem_id)
        if error_msg:
            return {'error': error_msg}, 404

        enable_o2, compare_mode = _parse_options(problem.compare_mode)
        config_data = _build_config(problem, examples, enable_o2, compare_mode)

        file_hash = calculate_file_sha256(temp_path)
        app.logger.info(f"Calculated file hash: {file_hash}")

        client = JudgeServerClient(SERVER_HOST, SERVER_PORT)
        results = client.send_code_and_config(temp_path, config_data, file_hash)
        if results is None:
            return {'error': 'Communication with JudgeServer failed'}, 500

        user_id = session.get('user_id')
        if not user_id:
            return {'error': 'User not logged in'}, 401

        submit_time = datetime.now()
        _save_to_db(user_id, problem_id, submit_time, results, temp_path)

        return {'status': 'ok', 'results': results}, 200

    except Exception as e:
        app.logger.error(f"Error in submit_solution: {e}")
        db.session.rollback()
        return {'error': str(e)}, 500

    finally:
        if temp_path:
            _cleanup(temp_path)
