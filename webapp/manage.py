from pathlib import Path
import click
import sys
import os

from app.cli.main import cli
from app.core.config import write_default_config

if __name__ == "__main__":
    if not Path("config.yml").exists():
        write_default_config()
        click.echo("[initialize] config.yml not found. Default configuration file has been generated.")
        click.echo("[initialize] Please edit config.yml before running BCMOJ again.")
        sys.exit(0)
    cli()