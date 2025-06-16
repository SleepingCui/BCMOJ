from logging.handlers import RotatingFileHandler
from datetime import datetime
from colorlog import ColoredFormatter
from app.config import config

import os
import logging
import contextvars

config = config.get_config()
log_route_context = contextvars.ContextVar("log_route_context", default="main")

class RouteNameFilter(logging.Filter):
    def filter(self, record):
        route_name = log_route_context.get()
        if route_name == 'nolog':
            return False
        record.route_name = route_name
        return True

def setup_logging(app=None):
    disable_color=config['app_settings']['disable_color_log']

    log_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'logs')
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
