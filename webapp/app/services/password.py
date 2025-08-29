import hashlib
import random
import smtplib
from email.mime.text import MIMEText
from flask import session, current_app as app
from app.core.db import db, User
from app.core.config import get_config

config = get_config()
EMAIL_CONFIG = config['email_config']

def send_verification_email(email, verification_code):
    try:
        msg = MIMEText(f'Your verification code is: {verification_code}', 'plain', 'utf-8')
        msg['Subject'] = 'Password Recovery Verification Code'
        msg['From'] = EMAIL_CONFIG['sender']
        msg['To'] = email

        smtp_port = int(EMAIL_CONFIG.get('smtp_port', 587))

        with smtplib.SMTP(EMAIL_CONFIG['smtp_server'], smtp_port) as server:
            server.starttls()
            server.login(EMAIL_CONFIG['sender'], EMAIL_CONFIG['password'])
            server.send_message(msg)
        return True
    except Exception as e:
        app.logger.error(f"Error sending email to {email}: {e}")
        return False

def generate_verification_code(length=6):
    return ''.join(str(random.randint(0, 99999)) for _ in range(length))

def start_password_reset(email):
    user = User.query.filter_by(email=email).first()
    if not user:
        return False, '未知的邮箱'

    verification_code = generate_verification_code()
    session['verification_code'] = verification_code
    session['verification_email'] = email

    if send_verification_email(email, verification_code):
        return True, '发送验证码成功，请检查您的邮箱'
    else:
        return False, '无法发送邮件，请稍后再试'

def verify_and_reset_password(user_code, new_password, confirm_password):
    if new_password != confirm_password:
        return False, '密码不匹配'

    if 'verification_code' not in session or 'verification_email' not in session:
        return False, 'Session expired. Please start again.'

    if user_code != session.get('verification_code'):
        return False, '未知的验证码'

    email = session.get('verification_email')
    user = User.query.filter_by(email=email).first()
    if not user:
        return False, '未找到用户'

    hashed_password = hashlib.sha256(new_password.encode()).hexdigest()
    user.passwd = hashed_password
    db.session.commit()

    session.pop('verification_code', None)
    session.pop('verification_email', None)

    return True, '密码更新成功'
