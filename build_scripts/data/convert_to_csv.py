import csv
import re


def convert_to_csv(input_filename, output_filename):
    with open(input_filename, mode='r', encoding='utf-8-sig') as inp:
        with open(output_filename, mode='w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)

            writer.writerow(['Character', 'Pinyin', 'Definition'])

            for line in inp:
                line = line.strip()
                if not line:
                    continue

                parts = [p.strip() for p in re.split(r'\t+|\s{2,}', line)]

                if len(parts) >= 3:
                    char = parts[0]
                    pinyin = parts[1]
                    definition = ' '.join(parts[2:])
                    writer.writerow([char, pinyin, definition])
                elif len(parts) == 2:
                    writer.writerow([parts[0], parts[1], ''])
                else:
                    print(f"Warning: Could not parse line -> {line}")


if __name__ == "__main__":
    input_file = 'hsk3.txt'
    output_file = 'hsk3.csv'

    convert_to_csv(input_file, output_file)
    print(f"Success! Converted to {output_file}")
