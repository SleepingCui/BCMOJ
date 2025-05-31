from pathlib import Path
from threading import Lock
from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap

class ConfigManager:
    def __init__(self):
        self.config_path = Path('config.yml')
        self.config = None
        self.lock = Lock()
        self.yaml = YAML()
        self.yaml.indent(mapping=2, sequence=4, offset=2)
        self.load_config()

    def get_default_config(self):
        config = {
            'db_config': {
                'db_name': 'bcmoj',
                'db_host': 'localhost',
                'db_password': 'password',
                'db_port': 3306,
                'db_user': 'root',
            },
            'email_config': {
                'email_password': 'password',
                'email_sender': 'example@example.com',
                'email_smtp_port': 587,
                'email_smtp_server': 'smtp.example.com',
            },
            'judge_config': {
                'enable_code_security_check': False,
                'judge_host': 'localhost',
                'judge_port': 12345,
            },
            'app_settings': {
                'secret_key': 'your_secret_key_here',
                'upload_folder': 'tmp',
                'userdata_folder': 'userdata',
                'uwsgi_stats_url': 'http://0.0.0.0:9191',
                'disable_color_log': False,
            }
        }
        def add_comments(d, comments):
            cm = CommentedMap()
            for k, v in d.items():
                cm[k] = v
            for k, c in comments.items():
                cm.yaml_add_eol_comment(c, key=k)
            return cm
        
        db_config_comments = {
            'db_name': '数据库名称',
            'db_host': '数据库主机地址',
            'db_password': '数据库用户密码',
            'db_port': '数据库端口号（默认3306）',
            'db_user': '数据库用户名',
        }
        config['db_config'] = add_comments(config['db_config'], db_config_comments)

        email_config_comments = {
            'email_password': '发件邮箱的授权码或密码',
            'email_sender': '发件人邮箱地址',
            'email_smtp_port': 'SMTP 端口，587 支持 TLS 加密',
            'email_smtp_server': '邮件发送服务器地址',
        }
        config['email_config'] = add_comments(config['email_config'], email_config_comments)

        judge_config_comments = {
            'enable_code_security_check': '是否启用代码安全检查（建议设置为 true）',
            'judge_host': '判题服务的主机地址',
            'judge_port': '判题服务监听端口',
        }
        config['judge_config'] = add_comments(config['judge_config'], judge_config_comments)

        app_settings_comments = {
            'secret_key': 'Flask 用于加密 session 的密钥，请设置为随机字符串',
            'upload_folder': '上传文件存储目录，临时存储提交代码等',
            'userdata_folder': '用户数据存储目录，包含用户文件、代码等',
            'uwsgi_stats_url': 'uWSGI 统计信息接口地址',
            'disable_color_log': '是否禁用彩色日志',
        }
        config['app_settings'] = add_comments(config['app_settings'], app_settings_comments)


        commented_config = CommentedMap(config)
        return commented_config

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

def get_config():
    return config_manager.get_config()
