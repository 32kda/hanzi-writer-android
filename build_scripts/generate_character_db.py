"""
Build-time script: generates the pre-populated characters.db for Room.

Usage:
    python generate_character_db.py --data-dir ../app/src/main/assets/data --sets-dir ../app/src/main/assets/sets --output ../app/src/main/assets/databases/characters.db

Reads MakeMeAHanzi-style JSON files (all.json or individual files),
produces a SQLite database with stroke data for fast rendering lookup.
"""

import json
import sqlite3
import argparse
import csv
import io
from pathlib import Path


def load_pinyin_map(sets_dir: str) -> dict:
    """Load pinyin and definitions from set CSV files into a unicode-keyed dict."""
    sets_dir = Path(sets_dir)
    pinyin_map = {}
    if not sets_dir.exists():
        print(f"Warning: sets directory not found: {sets_dir}")
        return pinyin_map

    for csv_file in sets_dir.rglob("*.csv"):
        try:
            with open(csv_file, "r", encoding="utf-8") as f:
                reader = csv.reader(f)
                for row in reader:
                    if len(row) >= 1:
                        char = row[0].strip()
                        if not char or len(char) != 1:
                            continue
                        pinyin = row[1].strip() if len(row) > 1 else ""
                        definition = row[2].strip() if len(row) > 2 else ""
                        unicode = ord(char)
                        if unicode not in pinyin_map:
                            pinyin_map[unicode] = (pinyin, definition)
        except Exception as e:
            print(f"Warning: error reading {csv_file}: {e}")

    print(f"Loaded pinyin/definition for {len(pinyin_map)} characters from sets")
    return pinyin_map


def build_database(data_dir: str, sets_dir: str, output_path: str, db_version: int = 4):
    data_dir = Path(data_dir)
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    pinyin_map = load_pinyin_map(sets_dir)

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

    print(f"Loaded {len(characters_data)} characters from JSON data")

    # Create database
    if output_path.exists():
        output_path.unlink()

    conn = sqlite3.connect(str(output_path))
    cursor = conn.cursor()

    cursor.executescript(f"""
        CREATE TABLE characters (
            unicode INTEGER NOT NULL PRIMARY KEY,
            char TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            definition TEXT NOT NULL
        );

        CREATE TABLE stroke_data (
            unicode INTEGER NOT NULL,
            stroke_index INTEGER NOT NULL,
            path_data TEXT NOT NULL,
            median_points TEXT NOT NULL,
            PRIMARY KEY (unicode, stroke_index)
        );

        CREATE TABLE character_progress (
            unicode INTEGER NOT NULL PRIMARY KEY,
            accuracy REAL NOT NULL,
            totalAttempts INTEGER NOT NULL,
            correctAttempts INTEGER NOT NULL,
            consecutiveCorrect INTEGER NOT NULL,
            lastPracticed INTEGER NOT NULL,
            lastResult TEXT NOT NULL,
            averageResponseTimeMs INTEGER NOT NULL,
            hintUsageCount INTEGER NOT NULL,
            introducedDate INTEGER NOT NULL,
            isLearned INTEGER NOT NULL,
            activeSetName TEXT NOT NULL,
            FOREIGN KEY (unicode) REFERENCES characters(unicode) ON DELETE CASCADE
        );

        CREATE TABLE daily_engagement (
            date TEXT NOT NULL PRIMARY KEY,
            totalTimeMinutes INTEGER NOT NULL,
            engagementLevel TEXT NOT NULL,
            activitiesCompleted TEXT NOT NULL,
            charactersLearned INTEGER NOT NULL,
            charactersDrilled INTEGER NOT NULL,
            charactersQuizzed INTEGER NOT NULL,
            quizScore INTEGER
        );

        CREATE TABLE streak (
            id INTEGER NOT NULL PRIMARY KEY,
            currentStreak INTEGER NOT NULL,
            longestStreak INTEGER NOT NULL,
            lastActiveDate TEXT NOT NULL
        );
    """)

    for char, char_data in characters_data.items():
        unicode = ord(char)
        strokes = char_data.get("strokes", [])
        medians = char_data.get("medians", [])
        pinyin, definition = pinyin_map.get(unicode, ("", ""))

        cursor.execute("""
            INSERT OR REPLACE INTO characters
            (unicode, char, pinyin, definition)
            VALUES (?, ?, ?, ?)
        """, (unicode, char, pinyin, definition))

        for i, stroke_path in enumerate(strokes):
            median_points = medians[i] if i < len(medians) else []
            cursor.execute("""
                INSERT INTO stroke_data (unicode, stroke_index, path_data, median_points)
                VALUES (?, ?, ?, ?)
            """, (unicode, i, stroke_path, json.dumps(median_points)))

    # Set the Room schema version in the database
    cursor.execute(f"PRAGMA user_version = {db_version}")

    conn.commit()
    conn.close()
    print(f"Database built: {output_path} (version {db_version})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build pre-populated character database")
    parser.add_argument("--data-dir", default="../app/src/main/assets/data", help="Path to character JSON data directory")
    parser.add_argument("--sets-dir", default="../app/src/main/assets/sets", help="Path to set CSV directories")
    parser.add_argument("--output", default="../app/src/main/assets/databases/characters.db", help="Output database path")
    args = parser.parse_args()

    build_database(args.data_dir, args.sets_dir, args.output)
