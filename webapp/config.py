import yaml
from pathlib import Path
def load_config():
    config_path = Path('config.yml')
    
    default_config = {
        'UPLOAD_FOLDER': 'tmp',
        'AVATAR_FOLDER': 'userdata',
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
        'SERVER_CONFIG': {
            'host': 'localhost',
            'port': 5000
        }
    }
    
    if not config_path.exists():
        with open(config_path, 'w') as f:
            yaml.dump(default_config, f)
        return default_config
    with open(config_path) as f:
        return yaml.safe_load(f)
