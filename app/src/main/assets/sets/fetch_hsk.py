import re
import csv
import sys
import os
import requests

def fetch_page(level, page):
    url = f"http://hanzidb.org/character-list/hsk/level-{level}?page={page}"
    resp = requests.get(url)
    resp.encoding = 'utf-8'
    return resp.text

def parse_rows(html):
    pattern = re.compile(
        r'<tr><td><a href="/character/[^"]+">([^<]+)</a></td>'
        r'<td>(?:<span[^>]*>)?([^<]+)(?:</span>)?</td>'
        r'<td><span class="smmr">([^<]*)</span></td>'
    )
    return pattern.findall(html)

def main():
    levels = {
        4: 5,
        5: 7,
        6: 10,
    }

    args = sys.argv[1:]
    if args:
        target_levels = [int(l) for l in args if l.isdigit() and int(l) in levels]
    else:
        target_levels = list(levels.keys())

    for level in target_levels:
        rows = []
        max_page = levels[level]
        for page in range(1, max_page + 1):
            html = fetch_page(level, page)
            page_rows = parse_rows(html)
            rows.extend(page_rows)
            print(f"Level {level}, page {page}: {len(page_rows)} characters")

        out_dir = os.path.join(os.path.dirname(__file__) or ".", f"hsk{level}")
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, f"hsk{level}.csv")

        with open(out_path, "w", newline="", encoding="utf-8") as f:
            writer = csv.writer(f, quoting=csv.QUOTE_ALL)
            for char, pinyin, defn in rows:
                writer.writerow([char, pinyin, defn])

        print(f"Level {level}: {len(rows)} characters written to {out_path}")

if __name__ == "__main__":
    main()
