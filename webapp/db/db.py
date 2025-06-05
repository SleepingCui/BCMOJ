from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
from webapp.config import config

# === 配置加载 ===
raw_db_config = config['db_config']

DB_URI = f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@" \
         f"{raw_db_config['db_host']}:{raw_db_config['db_port']}/{raw_db_config['db_name']}"

db = SQLAlchemy()

# === 模型定义 ===

class User(db.Model):
    __tablename__ = 'users'

    userid = db.Column(db.Integer, primary_key=True, autoincrement=True)
    username = db.Column(db.String(255), nullable=False)
    email = db.Column(db.String(255), nullable=False)
    passwd = db.Column(db.String(255), nullable=False)
    usergroup = db.Column(db.String(255), nullable=False)
    avatar = db.Column(db.String(255))


class Problem(db.Model):
    __tablename__ = 'problems'

    problem_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    title = db.Column(db.String(255), nullable=False)
    description = db.Column(db.Text, nullable=False)
    time_limit = db.Column(db.Integer, nullable=False, default=1000)


class JudgeResult(db.Model):
    __tablename__ = 'judge_results'

    result_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    userid = db.Column(db.Integer, db.ForeignKey('users.userid', ondelete='CASCADE'), nullable=False)
    problemid = db.Column(db.Integer, db.ForeignKey('problems.problem_id', ondelete='CASCADE'), nullable=False)
    time = db.Column(db.DateTime, default=datetime.utcnow)
    filepath = db.Column(db.String(1024))

    user = db.relationship('User', backref=db.backref('judge_results', lazy=True, cascade="all, delete"))
    problem = db.relationship('Problem', backref=db.backref('judge_results', lazy=True, cascade="all, delete"))


class CheckpointResult(db.Model):
    __tablename__ = 'checkpoint_results'

    result_id = db.Column(db.Integer, db.ForeignKey('judge_results.result_id', ondelete='CASCADE'), primary_key=True)
    checkpoint_id = db.Column(db.Integer, primary_key=True)
    result = db.Column(db.Integer)
    time = db.Column(db.Float)

    judge_result = db.relationship('JudgeResult', backref=db.backref('checkpoints', lazy=True, cascade="all, delete"))


class Example(db.Model):
    __tablename__ = 'examples'

    example_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    problem_id = db.Column(db.Integer, db.ForeignKey('problems.problem_id', ondelete='CASCADE'), nullable=False)
    input = db.Column(db.Text, nullable=False)
    output = db.Column(db.Text, nullable=False)

    problem = db.relationship('Problem', backref=db.backref('examples', lazy=True, cascade="all, delete"))
