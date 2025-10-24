# app/cli/db.py

import click
import sys
import os
from flask import Flask
from flask_migrate import init as migrate_init, migrate as flask_migrate, upgrade as flask_upgrade
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError, OperationalError

from app.core.db import get_db_engine
from app.cli.utils import check_database_exists


@click.group()
def db():
    """Database commands."""
    pass

@db.command()
def upgrade():
    """Automatically handle database migrations and upgrades."""
    click.echo("[DB-Upgrade] Starting database upgrade process...")

    from app.core.db import init_migrate
    from app.core.config import get_config
    config = get_config()
    db_name = config['db_config']['db_name']

    click.echo(f"[DB-Upgrade] Checking if database '{db_name}' exists...")
    db_exists, message = check_database_exists()
    if not db_exists:
        click.echo(f"[DB-Upgrade] {message}")
        click.echo(f"[DB-Upgrade] Database '{db_name}' not found or empty. Attempting to create/init it...")

        temp_app = Flask(__name__)
        try:
            from app.core.db import init_db
            init_db(temp_app)
            click.echo(f"[DB-Upgrade] Database '{db_name}' initialized successfully.")
        except Exception as e:
            click.echo(f"[DB-Upgrade] ERROR: Failed to initialize database: {e}")
            sys.exit(1)

        db_exists, message = check_database_exists()
        if not db_exists:
             click.echo(f"[DB-Upgrade] ERROR: Database '{db_name}' still not ready after init attempt: {message}")
             sys.exit(1)

    click.echo(f"[DB-Upgrade] {message}")

    app = Flask(__name__)
    init_migrate(app)

    migrations_dir = "migrations"
    with app.app_context():
        try:
            if not os.path.exists(migrations_dir):
                click.echo("[DB-Upgrade] Migrations directory not found. Initializing...")
                migrate_init()
                click.echo("[DB-Upgrade] Migration repository initialized.")

            versions_dir = os.path.join(migrations_dir, "versions")
            has_existing_migrations = os.path.exists(versions_dir) and len(os.listdir(versions_dir)) > 0

            if not has_existing_migrations:
                click.echo("[DB-Upgrade] No migration files found. Generating initial migration...")
                flask_migrate(message="Initial migration based on current models")
                click.echo("[DB-Upgrade] Initial migration generated.")
            else:
                click.echo("[DB-Upgrade] Checking for model changes and generating migration if needed...")
                try:
                    flask_migrate(message="Auto migration for model changes")
                    click.echo("[DB-Upgrade] New migration generated for model changes.")
                except SQLAlchemyError as e:
                    error_msg = str(e)
                    if "No changes in schema detected" in error_msg:
                        click.echo("[DB-Upgrade] No schema changes detected. No new migration needed.")
                    else:
                        click.echo(f"[DB-Upgrade] Migration generation check completed, potentially with warnings: {e}")

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
    config = get_config()
    db_name = config['db_config']['db_name']

    if not yes:
        if not click.confirm(
            f"[DB-Clear] WARNING: This will permanently delete the 'alembic_version' table "
            f"from database '{db_name}'. Use this ONLY if migrations folder was lost. Continue?",
            default=False
        ):
            click.echo("[DB-Clear] Operation cancelled.")
            return

    try:
        engine = get_db_engine(db_name=db_name)
        with engine.connect() as conn:
            trans = conn.begin()
            try:
                conn.execute(text("DROP TABLE IF EXISTS alembic_version;"))
                trans.commit()
                click.echo("[DB-Clear] 'alembic_version' table has been removed successfully.")
                click.echo("[DB-Clear] You can now re-run:")
                click.echo("    python manage.py db upgrade")
            except Exception as e:
                 trans.rollback()
                 raise e

    except Exception as e:
        click.echo(f"[DB-Clear] Error clearing 'alembic_version' table: {e}")
        sys.exit(1)


@db.command()
@click.argument("target", required=False)
@click.option("-y", "--yes", is_flag=True, help="Skip confirmation prompt")
def delete(target, yes):
    """
    Delete tables or entire database.

    \b
    Usage:
      python manage.py db delete           # Show available tables and usage
      python manage.py db delete <table>   # Delete a specific table
      python manage.py db delete all       # Delete the entire database
    """
    from app.core.config import get_config
    config = get_config()
    db_name = config['db_config']['db_name']

    if not target:
        try:
            engine = get_db_engine(db_name=db_name)
            with engine.connect() as conn:
                result = conn.execute(text("SHOW TABLES;"))
                tables = [row[0] for row in result.fetchall()]
                click.echo(f"[DB-Delete] Available tables in database '{db_name}':")
                for t in tables:
                    click.echo(f"  - {t}")
        except Exception as e:
            click.echo(f"[DB-Delete] Error listing tables: {e}")
            sys.exit(1)

        click.echo("\nUsage:")
        click.echo("  python manage.py db delete <table_name>")
        click.echo("  python manage.py db delete all")
        return

    if target.lower() == "all":
        if not yes:
            if not click.confirm(
                f"[DB-Delete] WARNING: This will permanently DROP DATABASE '{db_name}'. This action CANNOT be undone. Continue?",
                default=False
            ):
                click.echo("[DB-Delete] Operation cancelled.")
                return
        try:
            base_engine = get_db_engine(db_name=None)
            with base_engine.connect() as conn:
                trans = conn.begin()
                try:
                    conn.execute(text(f"DROP DATABASE IF EXISTS `{db_name}`;"))
                    trans.commit()
                    click.echo(f"[DB-Delete] Database '{db_name}' has been dropped successfully.")
                except Exception as e:
                     trans.rollback()
                     raise e

        except Exception as e:
            click.echo(f"[DB-Delete] Error dropping database '{db_name}': {e}")
            sys.exit(1)
        return

    try:
        engine = get_db_engine(db_name=db_name)
        with engine.connect() as conn:
            result = conn.execute(text("SHOW TABLES;"))
            existing_tables = [row[0] for row in result.fetchall()]
            if target not in existing_tables:
                click.echo(f"[DB-Delete] Error: Table '{target}' does not exist in database '{db_name}'.")
                return

            if not yes:
                if not click.confirm(
                    f"[DB-Delete] WARNING: This will permanently delete table '{target}' from database '{db_name}'. This action CANNOT be undone. Continue?",
                    default=False
                ):
                    click.echo("[DB-Delete] Operation cancelled.")
                    return

            trans = conn.begin()
            try:
                conn.execute(text(f"DROP TABLE IF EXISTS `{target}`;"))
                trans.commit()
                click.echo(f"[DB-Delete] Table '{target}' has been dropped successfully.")
            except Exception as e:
                 trans.rollback()
                 raise e

    except Exception as e:
        click.echo(f"[DB-Delete] Error dropping table '{target}': {e}")
        sys.exit(1)
