"""
Build-time script: generates the pre-populated characters.db for Room.

Usage:
    python generate_character_db.py

Reads MakeMeAHanzi-style JSON files (all.json),
produces a SQLite database with binary-encoded stroke data.
"""

import json
import sqlite3
import argparse
import csv
import struct
from pathlib import Path

CMD_MOVE_TO = 0
CMD_LINE_TO = 1
CMD_QUAD_TO = 2
CMD_CUBIC_TO = 3
CMD_CLOSE = 4


def encode_svg_path(path_str: str) -> bytes:
    """Parse SVG path string into binary: [cmd:u8] [coords:i16le...]"""
    buf = bytearray()
    parts = path_str.replace(",", " ").split()
    i = 0
    while i < len(parts):
        token = parts[i]
        if not token.isalpha():
            i += 1
            continue
        cmd_char = token.upper()
        i += 1

        if cmd_char == 'Z':
            buf.append(CMD_CLOSE)
            continue

        if cmd_char == 'M':
            cmd_byte = CMD_MOVE_TO
            coord_count = 2
        elif cmd_char == 'L':
            cmd_byte = CMD_LINE_TO
            coord_count = 2
        elif cmd_char == 'Q':
            cmd_byte = CMD_QUAD_TO
            coord_count = 4
        elif cmd_char == 'C':
            cmd_byte = CMD_CUBIC_TO
            coord_count = 6
        else:
            continue

        coords = []
        while len(coords) < coord_count and i < len(parts) and not parts[i].isalpha():
            try:
                coords.append(int(round(float(parts[i]))))
            except (ValueError, IndexError):
                pass
            i += 1

        if cmd_char == 'M':
            # First M pair is MoveTo
            if len(coords) >= 2:
                buf.append(CMD_MOVE_TO)
                buf.extend(struct.pack('<hh', coords[0], coords[1]))
                coords = coords[2:]
            # Remaining implicit pairs are LineTo
            while len(coords) >= 2:
                buf.append(CMD_LINE_TO)
                buf.extend(struct.pack('<hh', coords[0], coords[1]))
                coords = coords[2:]
        elif len(coords) >= coord_count:
            buf.append(cmd_byte)
            for c in coords[:coord_count]:
                buf.extend(struct.pack('<h', c))
            # Remaining pairs are implicit repeats of same command (for Q/C)
            coords = coords[coord_count:]
            while len(coords) >= coord_count:
                buf.append(cmd_byte)
                for c in coords[:coord_count]:
                    buf.extend(struct.pack('<h', c))
                coords = coords[coord_count:]
        elif len(coords) >= 2 and cmd_char == 'L':
            buf.append(cmd_byte)
            buf.extend(struct.pack('<hh', coords[0], coords[1]))

    return bytes(buf)


def encode_medians(medians_list: list) -> bytes:
    """Encode list of [[x,y],...] as binary: [i16 x, i16 y] pairs."""
    buf = bytearray()
    for point in medians_list:
        x = int(round(float(point[0])))
        y = int(round(float(point[1])))
        buf.extend(struct.pack('<hh', x, y))
    return bytes(buf)


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

    cursor.executescript("""
        CREATE TABLE characters (
            unicode INTEGER NOT NULL PRIMARY KEY,
            char TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            definition TEXT NOT NULL
        );

        CREATE TABLE stroke_data (
            unicode INTEGER NOT NULL,
            stroke_index INTEGER NOT NULL,
            path_data BLOB NOT NULL,
            median_points BLOB NOT NULL,
            PRIMARY KEY (unicode, stroke_index)
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
            path_bin = encode_svg_path(stroke_path)
            med_bin = encode_medians(median_points)
            cursor.execute("""
                INSERT INTO stroke_data (unicode, stroke_index, path_data, median_points)
                VALUES (?, ?, ?, ?)
            """, (unicode, i, sqlite3.Binary(path_bin), sqlite3.Binary(med_bin)))

    # Set the Room schema version in the database
    cursor.execute(f"PRAGMA user_version = {db_version}")

    conn.commit()
    conn.close()
    print(f"Database built: {output_path} (version {db_version})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build pre-populated character database")
    parser.add_argument("--data-dir", default="./build_scripts/data", help="Path to character JSON data directory")
    parser.add_argument("--sets-dir", default="./app/src/main/assets/sets", help="Path to set CSV directories")
    parser.add_argument("--output", default="./app/src/main/assets/databases/characters.db", help="Output database path")
    args = parser.parse_args()

    build_database(args.data_dir, args.sets_dir, args.output, db_version=2)
