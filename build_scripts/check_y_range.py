import json
import sys

DATA_PATH = r"D:\Work\git\hanzi-writer-android\build_scripts\data\all.json"

def main():
    with open(DATA_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    all_y = []
    y_by_char = {}  # char -> list of y values

    for char, entry in data.items():
        medians = entry.get("medians", [])
        char_ys = []
        for stroke_medians in medians:
            for point in stroke_medians:
                y = point[1]
                all_y.append(y)
                char_ys.append(y)
        if char_ys:
            y_by_char[char] = char_ys

    if not all_y:
        print("No median data found")
        return

    min_y = min(all_y)
    max_y = max(all_y)
    avg_y = sum(all_y) / len(all_y)

    print(f"Total characters with medians: {len(y_by_char)}")
    print(f"Total median points: {len(all_y)}")
    print()
    print(f"Y across all points:")
    print(f"  min  = {min_y}")
    print(f"  max  = {max_y}")
    print(f"  avg  = {avg_y:.1f}")
    print(f"  midpoint of range = {(min_y + max_y) / 2:.1f}")
    print()

    # Per-character min/avg/max
    top_5 = sorted(y_by_char.items(), key=lambda kv: sum(kv[1]) / len(kv[1]))
    print("Top 5 chars by lowest avg y (lowest on screen = top of char):")
    for char, ys in top_5:
        print(f"  U+{ord(char):04X}  min={min(ys):6.1f}  avg={sum(ys)/len(ys):6.1f}  max={max(ys):6.1f}")

    print()

    # Expected range based on description: y in [-124, 900], midpoint 388
    print("Expected per dataset spec: y in [-124, 900], midpoint = 388")
    print(f"Actual midpoint matches expected? {(min_y + max_y) / 2:.1f}")
    print(f"Y-flip applied in code: 1024 - y")
    print(f"  If raw y in [-124, 900], after flip: y' in [{1024-900}, {1024-(-124)}] = [124, 1148]")

if __name__ == "__main__":
    main()
