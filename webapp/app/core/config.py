from pathlib import Path
from threading import Lock
from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap, CommentedBase

class ConfigManager:
    def __init__(self):
        self.config_path = Path('config.yml')
        self.config = None
        self.lock = Lock()
        self.yaml = YAML()
        self.yaml.indent(mapping=2, sequence=4, offset=2)
        self.yaml.preserve_quotes = True
        self.load_config()

    def get_default_config(self):
        def section_comment(title):
            return f'# =========================================\n' \
                   f'# |         {title:<30}|\n' \
                   f'# =========================================\n'

        def add_section_header(cm: CommentedMap, comment: str):
            cm.yaml_set_start_comment(comment, indent=0)

        def add_comments(d: dict, comments: dict[str, str] = None):
            cm = CommentedMap()
            for k, v in d.items():
                cm[k] = v
            if comments:
                for k, c in comments.items():
                    cm.yaml_add_eol_comment(c, key=k)
            return cm
        config = CommentedMap()
        db_config = add_comments({
            'db_name': 'bcmoj',
            'db_host': 'localhost',
            'db_port': 3306,
            'db_user': 'root',
            'db_password': 'password'
        })
        add_section_header(db_config, section_comment("Database Configuration"))
        config['db_config'] = db_config

        email_config = add_comments({
            'email_sender': 'example@example.com',
            'email_password': 'password',
            'email_smtp_server': 'smtp.example.com',
            'email_smtp_port': 587
        }, {
            'email_smtp_port': 'TLS port'
        })
        add_section_header(email_config, section_comment("Email Configuration"))
        config['email_config'] = email_config

        judge_config = add_comments({
            'enable_code_security_check': False,
            'judge_host': 'localhost',
            'judge_port': 12345
        })
        add_section_header(judge_config, section_comment("Judge Configuration") +
                           "# Refer to: https://github.com/SleepingCui/BCMOJ/wiki/%E9%85%8D%E7%BD%AE")
        config['judge_config'] = judge_config

        app_settings = add_comments({
            'secret_key': 'your_secret_key_here',
            'upload_folder': 'tmp',
            'userdata_folder': 'userdata',
            'uwsgi_stats_url': 'http://0.0.0.0:9191',
            'disable_color_log': False
        }, {
            'secret_key': 'Used for session encryption, set to a strong random string',
            'uwsgi_stats_url': 'uWSGI stats server URL for monitoring'
        })
        add_section_header(app_settings, section_comment("Application Settings"))
        config['app_settings'] = app_settings

        return config

    def load_config(self):
        with self.lock:
            if not self.config_path.exists():
                with open(self.config_path, 'w', encoding='utf-8') as f:
                    self.yaml.dump(self.get_default_config(), f)
                self.config = self.get_default_config()
            else:
                with open(self.config_path, encoding='utf-8') as f:
                    self.config = self.yaml.load(f)

    def get_config(self):
        with self.lock:
            return self.config

config_manager = ConfigManager()
def get_config():    #入口
    return config_manager.get_config()
