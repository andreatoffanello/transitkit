#!/usr/bin/env python3
"""
Apply pipeline/schema.sql to the operator's Neon database.

Usage:
    python pipeline/apply_schema.py <operator_id>
    python pipeline/apply_schema.py appalcart
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

import psycopg2
from dotenv import load_dotenv

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCHEMA_FILE = SCRIPT_DIR / "schema.sql"


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python pipeline/apply_schema.py <operator_id>")
        sys.exit(1)

    operator_id = sys.argv[1]
    env_path = REPO_ROOT / "shared" / "operators" / operator_id / ".env"

    if not env_path.exists():
        print(f"ERROR: .env file not found at {env_path}")
        print(f"       Copy {env_path.parent}/.env.example and fill in DATABASE_URL.")
        sys.exit(1)

    load_dotenv(env_path)
    database_url = os.environ.get("DATABASE_URL")

    if not database_url:
        print(f"ERROR: DATABASE_URL not set in {env_path}")
        sys.exit(1)

    schema_sql = SCHEMA_FILE.read_text(encoding="utf-8")

    print(f"Connecting to Neon for operator '{operator_id}'...")
    conn = psycopg2.connect(database_url)
    conn.autocommit = True
    try:
        with conn.cursor() as cur:
            cur.execute(schema_sql)
        print("Schema applied successfully.")

        with conn.cursor() as cur:
            cur.execute("SELECT version FROM schema_version;")
            row = cur.fetchone()
            print(f"schema_version = {row[0]}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
