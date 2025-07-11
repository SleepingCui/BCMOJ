from .db import db, DB_URI, init_db
from .db import User, Problem, JudgeResult, CheckpointResult, Example
from .logger import setup_logging, log_route_context

version = "1.0.5-alpha"

__all__ = [
    'db',
    'DB_URI',
    'User',
    'Problem',
    'JudgeResult',
    'CheckpointResult',
    'Example'
]
