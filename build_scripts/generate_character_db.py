"""
Build-time script: generates the pre-populated characters.db for Room.

Usage:
    python generate_character_db.py --data-dir ../app/src/main/assets/data --output ../app/src/main/assets/databases/characters.db

Reads MakeMeAHanzi-style JSON files (all.json or individual files),
produces a SQLite database with stroke data for fast rendering lookup.
"""

import json
import sqlite3
import argparse
from pathlib import Path


def build_database(data_dir: str, output_path: str):
    data_dir = Path(data_dir)
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # Load all character data from JSON files
    characters_data = {}
    all_json = data_dir / "all.json"

    if all_json.exists():
        with open(all_json, "r", encoding="utf-8") as f:
            content = f.read()
            try:
                all_data = json.loads(content)
                if isinstance(all_data, list):
                    for entry in all_data:
                        char = entry.get("character", "")
                        if char:
                            characters_data[char] = entry
                elif isinstance(all_data, dict):
                    for char, data in all_data.items():
                        if len(char) == 1:
                            characters_data[char] = data
            except json.JSONDecodeError:
                print(f"Warning: could not parse {all_json}")
    else:
        json_files = list(data_dir.glob("*.json"))
        for json_file in json_files:
            try:
                with open(json_file, "r", encoding="utf-8") as f:
                    entry = json.load(f)
                char = entry.get("character", "")
                if char:
                    characters_data[char] = entry
            except (json.JSONDecodeError, IOError):
                continue

    print(f"Loaded {len(characters_data)} characters")

    # Create database
    if output_path.exists():
        output_path.unlink()

    conn = sqlite3.connect(str(output_path))
    cursor = conn.cursor()

    cursor.executescript("""
        CREATE TABLE characters (
            unicode INTEGER NOT NULL PRIMARY KEY,
            char TEXT NOT NULL
        );

        CREATE TABLE stroke_data (
            unicode INTEGER NOT NULL,
            stroke_index INTEGER NOT NULL,
            path_data TEXT NOT NULL,
            median_points TEXT NOT NULL,
            PRIMARY KEY (unicode, stroke_index)
        );
    """)

    for char, char_data in characters_data.items():
        unicode = ord(char)
        strokes = char_data.get("strokes", [])
        medians = char_data.get("medians", [])

        cursor.execute("""
            INSERT OR REPLACE INTO characters
            (unicode, char)
            VALUES (?, ?)
        """, (unicode, char))

        for i, stroke_path in enumerate(strokes):
            median_points = medians[i] if i < len(medians) else []
            cursor.execute("""
                INSERT INTO stroke_data (unicode, stroke_index, path_data, median_points)
                VALUES (?, ?, ?, ?)
            """, (unicode, i, stroke_path, json.dumps(median_points)))

    conn.commit()
    conn.close()
    print(f"Database built: {output_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build pre-populated character database")
    parser.add_argument("--data-dir", default="../app/src/main/assets/data", help="Path to character JSON data directory")
    parser.add_argument("--output", default="../app/src/main/assets/databases/characters.db", help="Output database path")
    args = parser.parse_args()

    build_database(args.data_dir, args.output)
