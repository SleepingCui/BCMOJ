import click
from app.cli.utils import logo
from app.core import version
from app.cli.run import run
from app.cli.db import db

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
        click.echo(logo)
        click.echo(f"Version : {version}")
        click.echo(ctx.get_help())

cli.add_command(run)
cli.add_command(db)