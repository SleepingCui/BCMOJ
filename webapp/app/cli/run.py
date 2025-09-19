import click
import sys
from werkzeug.serving import run_simple

from app.cli.utils import logo
from app.core.logger import CustomRequestHandler

@click.command("run", short_help="Start the BCMOJ web server.")
@click.option("--host", default="127.0.0.1", help="Host to listen on (default: 127.0.0.1)")
@click.option("--port", default=5000, type=int, help="Port to listen on (default: 5000)")
@click.option("--wsgi", is_flag=True, help="Use WSGI server (gunicorn or waitress)")
@click.option("--debug", is_flag=True, help="Enable debug mode (cannot be used with --wsgi)")
def run(host, port, wsgi, debug):
    """Run the BCMOJ web server."""
    click.echo(logo)
    
    if wsgi and debug:
        click.echo("[initialize] Error: --wsgi and --debug cannot be used together")
        sys.exit(1)
    
    from app.app import app  
    from app.core import version
    from app.core.logger import setup_logging
    
    setup_logging(app)
    click.echo(f"[initialize] App Version : {version}")

    if wsgi:
        try:
            import gunicorn.app.wsgiapp as gunicorn_app
            from app.core.config import get_config
            click.echo(f"[initialize] Starting WSGI server: gunicorn on http://{host}:{port}")
            sys.argv = [
                "gunicorn",
                "app.app:app",
                "-b", f"{host}:{port}",
                "--access-logfile", "-",
                "--log-level", "info",
                "--workers", str(get_config()['app_settings']['gunicorn_workers'])
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
                click.echo("    pip install gunicorn  (for Linux/Unix)")
                click.echo("    pip install waitress  (for Windows)")
                sys.exit(1)

    else:
        if debug:
            click.echo(f"[initialize] Starting development server on http://{host}:{port}")
            click.echo("[initialize] Debug mode: ON")
        else:
            click.echo(f"[initialize] Starting development server on http://{host}:{port}")
            click.echo("[initialize] Debug mode: OFF")
            
        run_simple(hostname=host, port=port, application=app,
                   use_reloader=debug, use_debugger=debug,
                   request_handler=CustomRequestHandler)