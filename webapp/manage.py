from werkzeug.serving import run_simple, WSGIRequestHandler
import logging
import click
import sys

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
        try:
            from app.logger import log_route_context
            route_name = log_route_context.get('werkzeug') if log_route_context.get() == 'main' else log_route_context.get()
        except ImportError:
            route_name = 'app'
        logger.log(level, '[%s] %s - %s', route_name, client_ip, message)


@click.group(invoke_without_command=True)
@click.pass_context
def cli(ctx):
    """
    BCMOJ Command Line Interface
    
    \b
    Usage:
      python manage.py run --host=HOST --port=PORT [--wsgi]
    
    \b
    Examples:
      python manage.py run --host=0.0.0.0 --port=80 --wsgi
      python manage.py run --host=127.0.0.1 --port=5000
      python manage.py run --host=0.0.0.0 --port=8080 --wsgi
    """
    if ctx.invoked_subcommand is None:
        click.echo(logo)
        click.echo(ctx.get_help())


@cli.command("run", short_help="Start the BCMOJ web server.")
@click.option("--host", default="127.0.0.1", help="Host to listen on (default: 127.0.0.1)")
@click.option("--port", default=5000, type=int, help="Port to listen on (default: 5000)")
@click.option("--wsgi", is_flag=True, help="Use WSGI server (gunicorn or waitress)")
@click.option("--debug", is_flag=True, help="Enable debug mode (cannot be used with --wsgi)")
def run(host, port, wsgi, debug):
    """Run the BCMOJ web server.
    
    \b
    Examples:
      python manage.py run --host=0.0.0.0 --port=80 --wsgi
      python manage.py run --host=127.0.0.1 --port=5000 --debug
    """
    click.echo(logo)
    
    if wsgi and debug:
        click.echo("[initialize] Error: --wsgi and --debug cannot be used together")
        sys.exit(1)
    
    from app.app import app  
    from app.logger import setup_logging
    
    setup_logging(app)
    
    if wsgi:
        try:
            import gunicorn.app.wsgiapp as gunicorn_app
            click.echo(f"[initialize] Starting WSGI server: gunicorn on http://{host}:{port}")
            sys.argv = [
                "gunicorn",
                f"{app.__module__}:app",
                "-b", f"{host}:{port}",
                "--access-logfile", "-",
                "--log-level", "info",
                "--workers", "4"
            ]
            gunicorn_app.run()
        except ImportError:
            try:
                from waitress import serve
                click.echo(f"[initialize] gunicorn not found, using waitress at http://{host}:{port}")
                
                app.logger.warning("Waitress does not support request logging. "
                                 "Access logs will NOT be recorded unless handled manually.")
                                 
                serve(app, host=host, port=port)
            except ImportError:
                click.echo("[initialize] Neither gunicorn nor waitress is installed.")
                click.echo("Please install one of them:")
                click.echo("    pip install gunicorn")
                click.echo("    pip install waitress")
                sys.exit(1)
    else:
        if debug:
            click.echo(f"[initialize] Starting development server on http://{host}:{port}")
            click.echo("[initialize] Debug mode: ON")
        else:
            click.echo(f"[initialize] Starting development server on http://{host}:{port}")
            click.echo("[initialize] Debug mode: OFF")
            
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