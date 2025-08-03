from logging.handlers import RotatingFileHandler
from werkzeug.serving import WSGIRequestHandler
from datetime import datetime
from colorlog import ColoredFormatter
import os
import logging
import contextvars

from .config import get_config

config = get_config()
log_route_context = contextvars.ContextVar("log_route_context", default="main")

class RouteNameFilter(logging.Filter):
    def filter(self, record):
        route_name = log_route_context.get()
        if route_name == 'nolog':
            return False
        record.route_name = route_name
        return True

def get_project_root():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.abspath(os.path.join(current_dir, '../../../'))

def setup_logging(app=None):
    disable_color = config['app_settings']['disable_color_log']
    
    project_root = get_project_root()
    log_dir = os.path.join(project_root, 'logs')
    os.makedirs(log_dir, exist_ok=True)
    log_file = os.path.join(log_dir, f"web-{datetime.now().strftime('%Y-%m-%d')}.log")

    log_format = '[%(asctime)s] [%(levelname)s] %(message)s'
    file_formatter = logging.Formatter(log_format)

    file_handler = RotatingFileHandler(
        log_file, maxBytes=10 * 1024 * 1024, backupCount=5, encoding='utf-8'
    )
    file_handler.setFormatter(file_formatter)
    file_handler.setLevel(logging.INFO)
    file_handler.addFilter(RouteNameFilter())

    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.addFilter(RouteNameFilter())

    if not disable_color:
        color_formatter = ColoredFormatter(
            '%(log_color)s' + log_format + '%(reset)s',
            datefmt='%Y-%m-%d %H:%M:%S',
            reset=True,
            log_colors={
                'DEBUG': 'cyan',
                'INFO': 'green',
                'WARNING': 'yellow',
                'ERROR': 'red',
                'CRITICAL': 'red,bg_white',
            },
            style='%'
        )
        console_handler.setFormatter(color_formatter)
    else:
        console_handler.setFormatter(file_formatter)

    if app:
        for handler in list(app.logger.handlers):
            app.logger.removeHandler(handler)
        app.logger.addHandler(file_handler)
        app.logger.addHandler(console_handler)
        app.logger.setLevel(logging.INFO)

    werkzeug_logger = logging.getLogger('werkzeug')
    werkzeug_logger.handlers.clear()
    werkzeug_logger.addHandler(file_handler)
    werkzeug_logger.addHandler(console_handler)
    werkzeug_logger.setLevel(logging.INFO)
    werkzeug_logger.addFilter(RouteNameFilter())

class CustomRequestHandler(WSGIRequestHandler):
    def log(self, type, message, *args):
        if args:
            message = message % args
        logger = logging.getLogger('werkzeug')
        level = {
            'info': logging.INFO,
            'warning': logging.WARNING,
            'error': logging.ERROR,
        }.get(type, logging.INFO)
        client_ip = self.client_address[0]
        try:
            from app.core.logger import log_route_context
            route_name = log_route_context.get('werkzeug') if log_route_context.get() == 'main' else log_route_context.get()
        except ImportError:
            route_name = 'app'
        logger.log(level, '[%s] %s - %s', route_name, client_ip, message)