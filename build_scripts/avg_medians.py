import json
import sys
import math

DATA_PATH = r"D:\Work\git\hanzi-writer-android\build_scripts\data\all.json"

TARGETS = [
    ("\u5341", "shi (ten)", "U+5341"),
    ("\u7c73", "mi (rice)", "U+7C73"),
]

def main():
    with open(DATA_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    for char, name, codepoint in TARGETS:
        entry = data.get(char)
        if entry is None:
            print(f"{name} ({codepoint}): NOT FOUND in data")
            continue

        medians = entry.get("medians", [])
        if not medians:
            print(f"{name} ({codepoint}): no medians")
            continue

        all_x = []
        all_y = []
        for i, stroke_medians in enumerate(medians):
            for point in stroke_medians:
                all_x.append(point[0])
                all_y.append(point[1])

        avg_x = sum(all_x) / len(all_x)
        avg_y = sum(all_y) / len(all_y)

        print(f"{name} ({codepoint}):")
        print(f"  strokes: {len(medians)}")
        print(f"  total median points: {len(all_x)}")
        print(f"  avg_x = {avg_x:.1f}")
        print(f"  avg_y = {avg_y:.1f}")
        print()

if __name__ == "__main__":
    main()
