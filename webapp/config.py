import yaml
from pathlib import Path
from threading import Lock
from flask import current_app

class ConfigManager:
    def __init__(self):
        self.config_path = Path('config.yml')
        self.config = None
        self.lock = Lock()
        self.load_config()

    def get_default_config(self):
        return {
            'APP_CONFIG': {
                'app_host': '0.0.0.0',
                'app_port': 80
            },
            'UPLOAD_FOLDER': 'tmp',
            'USERDATA_FOLDER': 'userdata',
            'SECRET_KEY': 'your_secret_key_here',
            'EMAIL_CONFIG': {
                'sender': 'your_email@example.com',
                'password': 'your_email_password',
                'smtp_server': 'smtp.example.com',
                'smtp_port': 587
            },
            'DB_CONFIG': {
                'host': 'localhost',
                'port': 3306,
                'user': 'root',
                'password': 'password',
                'database': 'bcmoj'
            },
            'JUDGE_CONFIG': {
                'host': 'localhost',
                'port': 12345,
                'enableCodeSecurityCheck': False
            }
        }

    def load_config(self):
        with self.lock:
            if not self.config_path.exists():
                current_app.logger.info("Writing config file...")
                with open(self.config_path, 'w') as f:
                    yaml.dump(self.get_default_config(), f)
                self.config = self.get_default_config()
            else:
                with open(self.config_path) as f:
                    self.config = yaml.safe_load(f)

    def get_config(self):
        with self.lock:
            return self.config

config_manager = ConfigManager()

def get_config():
    return config_manager.get_config()
