from flask.cli import FlaskGroup
from werkzeug.serving import run_simple, WSGIRequestHandler
from app.app import app, log_route_context
import logging
import click

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

    \b
    Examples:
      python webapp_run.py runserver
      python webapp_run.py runserver --host 0.0.0.0 --port 8080 --debug     
    """
    if ctx.invoked_subcommand is None:
        click.echo(logo)
        click.echo(cli.get_help(ctx))

@cli.command("runserver", short_help="Start the BCMOJ web server.")
@click.option("--host", default="127.0.0.1", help="Host to listen on (default: 127.0.0.1)")
@click.option("--port", default=5000, help="Port to listen on (default: 5000)")
@click.option("--debug", is_flag=True, help="Enable debug mode with reloader")
def runserver(host, port, debug):
    """Run the BCMOJ development web server with custom logging and options."""
    click.echo(logo)
    click.echo(f"→ Running on http://{host}:{port}/ (Press CTRL+C to quit)")
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
