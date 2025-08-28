from werkzeug.serving import run_simple
from app.core.config import write_default_config
from app.core.logger import CustomRequestHandler
from pathlib import Path
import logging
import click
import sys
import os

logo = r"""
  ____   ____ __  __  ___      _   ____            _           _    
 | __ ) / ___|  \/  |/ _ \    | | |  _ \ _ __ ___ (_) ___  ___| |_  
 |  _ \| |   | |\/| | | | |_  | | | |_) | '__/ _ \| |/ _ \/ __| __| 
 | |_) | |___| |  | | |_| | |_| | |  __/| | | (_) | |  __| (__| |_ 
 |____/ \____|_|  |_|\___/ \___/  |_|   |_|  \____/ |\___|\___|\__|
                                                 |__/              
  Developed by SleepingCui    https://github.com/SleepingCui/BCMOJ/
"""

@click.group(invoke_without_command=True)
@click.pass_context
def cli(ctx):
    """
    BCMOJ Command Line Interface
    
    \b
    Usage:
      python manage.py run --host=HOST --port=PORT [--wsgi]
      python manage.py db upgrade
      python manage.py db clear
    
    \b
    Examples:
      python manage.py run --host=0.0.0.0 --port=80 --wsgi
      python manage.py run --host=127.0.0.1 --port=5000
      python manage.py db upgrade
      python manage.py db clear
    """
    if ctx.invoked_subcommand is None:
        from app.core import version
        click.echo(logo)
        click.echo(f"Version : {version}")
        click.echo(ctx.get_help())


@cli.command("run", short_help="Start the BCMOJ web server.")
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


def check_database_exists():
    try:
        from app.core.config import get_config
        from sqlalchemy import create_engine, text
        
        config = get_config()
        raw_db_config = config['db_config']
        base_uri = f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@" \
                   f"{raw_db_config['db_host']}:{raw_db_config['db_port']}"
        
        engine = create_engine(base_uri, echo=False)
        
        with engine.connect() as conn:
            result = conn.execute(text(
                f"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '{raw_db_config['db_name']}'"
            ))
            db_exists = result.fetchone() is not None
            
            if not db_exists:
                return False, f"Database '{raw_db_config['db_name']}' does not exist"
            full_uri = f"{base_uri}/{raw_db_config['db_name']}"
            db_engine = create_engine(full_uri, echo=False)
            
            with db_engine.connect() as db_conn:
                result = db_conn.execute(text("SHOW TABLES"))
                tables = result.fetchall()
                
                if not tables:
                    return False, f"Database '{raw_db_config['db_name']}' exists but has no tables"
                
                return True, f"Database '{raw_db_config['db_name']}' exists with {len(tables)} tables"
                
    except Exception as e:
        return False, f"Error checking database: {e}"


@cli.group()
def db():
    """Database commands."""
    pass


@db.command()
def upgrade():
    """Automatically handle database migrations and upgrades."""
    from flask import Flask
    from app.core.db import init_migrate
    from flask_migrate import init, migrate as flask_migrate, upgrade as flask_upgrade
    import shutil
    
    click.echo("[DB-Upgrade] Starting database upgrade process...")
    db_exists, message = check_database_exists()
    if not db_exists:
        click.echo(f"[DB-Upgrade] {message}")
        click.echo("[DB-Upgrade] ERROR: Database not found or empty!")
        click.echo("")
        click.echo("Please run the web server first to initialize the database:")
        click.echo("  python manage.py run --port=5000")
        click.echo("")
        click.echo("After the web server starts successfully, stop it (Ctrl+C) and then run:")
        click.echo("  python manage.py db upgrade")
        sys.exit(1)
    click.echo(f"[DB-Upgrade] {message}")
    app = Flask(__name__)
    init_migrate(app)
    migrations_dir = "migrations"
    with app.app_context():
        try:
            if not os.path.exists(migrations_dir):
                click.echo("[DB-Upgrade] Migrations directory not found. Initializing...")
                init()
                click.echo("[DB-Upgrade] Migration repository initialized.")
            versions_dir = os.path.join(migrations_dir, "versions")
            if not os.path.exists(versions_dir) or len(os.listdir(versions_dir)) == 0:
                click.echo("[DB-Upgrade] No migration files found. Generating initial migration...")
                flask_migrate(message="Initial migration with example_visible_count field")
                click.echo("[DB-Upgrade] Initial migration generated.")
            else:
                click.echo("[DB-Upgrade] Checking for model changes...")
                try:
                    flask_migrate(message="Auto migration for model changes")
                    click.echo("[DB-Upgrade] New migration generated for model changes.")
                except Exception as e:
                    if "No changes in schema detected" in str(e):
                        click.echo("[DB-Upgrade] No schema changes detected.")
                    else:
                        click.echo(f"[DB-Upgrade] Migration generation completed: {e}")
            click.echo("[DB-Upgrade] Applying migrations...")
            flask_upgrade()
            click.echo("[DB-Upgrade] Database upgraded successfully.")
            
        except Exception as e:
            click.echo(f"[DB-Upgrade] Error during database upgrade: {e}")
            sys.exit(1)


@db.command()
@click.option("-y", "--yes", is_flag=True, help="Skip confirmation prompt")
def clear(yes):
    """
    Clear alembic_version table from the database.
    
    Use this ONLY if you accidentally deleted the migrations folder and
    see errors like:
        ERROR [flask_migrate] Error: Can't locate revision identified by 'xxxx'
    """
    from app.core.config import get_config
    from sqlalchemy import create_engine, text

    try:
        config = get_config()
        raw_db_config = config['db_config']
        db_name = raw_db_config['db_name']
        full_uri = (
            f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@"
            f"{raw_db_config['db_host']}:{raw_db_config['db_port']}/{db_name}"
        )

        if not yes:
            if not click.confirm(
                f"[DB-Clear] WARNING: This will permanently delete the alembic_version table "
                f"from database '{db_name}'. Use this ONLY if migrations folder was lost. Continue?",
                default=False
            ):
                click.echo("[DB-Clear] Operation cancelled.")
                return

        engine = create_engine(full_uri, echo=False)
        with engine.connect() as conn:
            conn.execute(text("DROP TABLE IF EXISTS alembic_version;"))
            conn.commit()

        click.echo("[DB-Clear] alembic_version table has been removed successfully.")
        click.echo("[DB-Clear] Now you can re-run:")
        click.echo("    python manage.py db upgrade")

    except Exception as e:
        click.echo(f"[DB-Clear] Error clearing alembic_version table: {e}")
        sys.exit(1)
    
@db.command()
@click.argument("target", required=False)
@click.option("-y", "--yes", is_flag=True, help="Skip confirmation prompt")
def delete(target, yes):
    """
    Delete tables or entire database.

    \b
    Usage:
      python manage.py db delete           # show help + list tables
      python manage.py db delete <table>   # delete a specific table
      python manage.py db delete all       # delete the entire database
    """
    from app.core.config import get_config
    from sqlalchemy import create_engine, text

    try:
        config = get_config()
        raw_db_config = config['db_config']
        db_name = raw_db_config['db_name']
        full_uri = (
            f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@"
            f"{raw_db_config['db_host']}:{raw_db_config['db_port']}/{db_name}"
        )
        engine = create_engine(full_uri, echo=False)
        if not target:
            with engine.connect() as conn:
                result = conn.execute(text("SHOW TABLES;"))
                tables = [row[0] for row in result.fetchall()]
                click.echo("[DB-Delete] Available tables:")
                for t in tables:
                    click.echo(f"  - {t}")
            click.echo("\nUsage:")
            click.echo("  python manage.py db delete <table>")
            click.echo("  python manage.py db delete all")
            return
        if target.lower() == "all":
            if not yes:
                if not click.confirm(
                    f"[DB-Delete] WARNING: This will permanently DROP DATABASE '{db_name}'. Continue?",
                    default=False
                ):
                    click.echo("[DB-Delete] Operation cancelled.")
                    return
            base_uri = (
                f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@"
                f"{raw_db_config['db_host']}:{raw_db_config['db_port']}"
            )
            base_engine = create_engine(base_uri, echo=False)
            with base_engine.connect() as conn:
                conn.execute(text(f"DROP DATABASE IF EXISTS {db_name};"))
                conn.commit()
            click.echo(f"[DB-Delete] Database '{db_name}' has been dropped successfully.")
            return
        with engine.connect() as conn:
            result = conn.execute(text("SHOW TABLES;"))
            tables = [row[0] for row in result.fetchall()]
            if target not in tables:
                click.echo(f"[DB-Delete] Table '{target}' does not exist in database '{db_name}'.") 
                return
            if not yes:
                if not click.confirm(
                    f"[DB-Delete] WARNING: This will permanently delete table '{target}' from database '{db_name}'. Continue?",
                    default=False
                ):
                    click.echo("[DB-Delete] Operation cancelled.")
                    return
            conn.execute(text(f"DROP TABLE IF EXISTS `{target}`;"))
            conn.commit()
        click.echo(f"[DB-Delete] Table '{target}' has been dropped successfully.")

    except Exception as e:
        click.echo(f"[DB-Delete] Error: {e}")
        sys.exit(1)



if __name__ == "__main__":
    if not Path("config.yml").exists():
        write_default_config()
        click.echo("[initialize] config.yml not found. Default configuration file has been generated.")
        click.echo("[initialize] Please edit config.yml before running BCMOJ again.")
        sys.exit(0)
    cli()
