from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from datetime import datetime
from sqlalchemy import create_engine, text
import random
import string
import hashlib
from .config import get_config

config = get_config()
raw_db_config = config['db_config']

DB_URI = f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@{raw_db_config['db_host']}:{raw_db_config['db_port']}/{raw_db_config['db_name']}"

db = SQLAlchemy()
migrate = Migrate()

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
    example_visible_count = db.Column(db.Integer, nullable=False, default=2)


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
    

def init_db(app): #create db if not exist
    db_name = raw_db_config['db_name']
    base_uri = f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@" \
               f"{raw_db_config['db_host']}:{raw_db_config['db_port']}"

    engine = create_engine(base_uri, echo=False)
    with engine.connect() as conn:
        conn.execute(text(f"CREATE DATABASE IF NOT EXISTS `{db_name}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"))
        conn.commit()

    app.config['SQLALCHEMY_DATABASE_URI'] = DB_URI
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    db.init_app(app)
    migrate.init_app(app, db)

    with app.app_context():
        db.create_all()
        admin_user = User.query.filter_by(username='admin').first()
        if not admin_user:
            generated_password = ''.join(random.choice(string.ascii_lowercase) for _ in range(10))
            hashed_password = hashlib.sha256(generated_password.encode()).hexdigest()
            admin_user = User(
                username='admin',
                email='admin@example.com',
                passwd=hashed_password,
                avatar=None,
                usergroup='admin'
            )
            db.session.add(admin_user)
            db.session.commit()

            app.logger.info(f"[DB] Default admin user created: username=admin password={generated_password} email=admin@example.com")


def init_migrate(app): #update
    app.config['SQLALCHEMY_DATABASE_URI'] = DB_URI
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    db.init_app(app)
    migrate.init_app(app, db)
