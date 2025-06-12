from werkzeug.serving import run_simple, WSGIRequestHandler
from app.app import app
from app.logger import setup_logging ,log_route_context
from flask.cli import FlaskGroup


import logging
import click
import sys

setup_logging(app)

logo = r"""
  ____   ____ __  __  ___      _   ____            _           _   
 | __ ) / ___|  \/  |/ _ \    | | |  _ \ _ __ ___ (_) ___  ___| |_ 
 |  _ \| |   | |\/| | | | |_  | | | |_) | '__/ _ \| |/ _ \/ __| __| 
 | |_) | |___| |  | | |_| | |_| | |  __/| | | (_) | |  __| (__| |_ 
 |____/ \____|_|  |_|\___/ \___/  |_|   |_|  \____/ |\___|\___|\__|
                                                |__/               
 Developed by SleepingCui    https://github.com/SleepingCui/BCMOJ/
"""

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
        route_name = log_route_context.get('werkzeug') if log_route_context.get() == 'main' else log_route_context.get()
        logger.log(level, '[%s] %s - %s', route_name, client_ip, message)

@click.group(cls=FlaskGroup, create_app=lambda: app, invoke_without_command=True)
@click.pass_context
def cli(ctx):
    """
    BCMOJ Command Line Interface

    \b
    Options for runserver:
      --host      Host to listen on (default: 127.0.0.1)
      --port      Port to listen on (default: 5000)
      --debug     Enable debug mode (with auto-reload)
      --wsgi      Use production WSGI server (waitress or gunicorn)

    \b
    Examples:
      python manage.py runserver
      python manage.py runserver --host 0.0.0.0 --port 8080 --debug
      python manage.py runserver --wsgi
    """
    if ctx.invoked_subcommand is None:
        click.echo(logo)
        click.echo(cli.get_help(ctx))

@cli.command("runserver", short_help="Start the BCMOJ web server.")
@click.option("--host", default="127.0.0.1", help="Host to listen on (default: 127.0.0.1)")
@click.option("--port", default=5000, help="Port to listen on (default: 5000)")
@click.option("--debug", is_flag=True, help="Enable debug mode with reloader")
@click.option("--wsgi", is_flag=True, help="Use WSGI server (gunicorn or waitress) instead of dev server")
def runserver(host, port, debug, wsgi):
    """Run the BCMOJ web server with custom logging and options."""
    click.echo(logo)

    if wsgi:
        try:
            import gunicorn.app.wsgiapp as gunicorn_app
            click.echo(f"Starting WSGI server: gunicorn on http://{host}:{port}")
            sys.argv = [
                "gunicorn",
                f"{app.__module__}:app",
                "-b", f"{host}:{port}",
                "--access-logfile", "-",
                "--log-level", "info"
            ]
            gunicorn_app.run()
        except ImportError:
            try:
                from waitress import serve
                click.echo(f"gunicorn not found, using waitress at http://{host}:{port}")
                
                app.logger.warning("Waitress does not support werkzeug-style request logging. "
                                   "Access logs will NOT be recorded unless handled manually.")
                
                serve(app, host=host, port=port)
            except ImportError:
                click.echo("Neither gunicorn nor waitress is installed.")
                exit(1)

    else:
        run_simple(
            hostname=host,
            port=port,
            application=app,
            use_reloader=debug,
            use_debugger=debug,
            request_handler=CustomRequestHandler
        )

if __name__ == "__main__":
    cli()
