"""Verify both reference samples and emit a portable, deterministic JSON report."""
import argparse
import hashlib
import json
from pathlib import Path

import main_image
import lens_data


SAMPLES = (
    ("D90_0101.bin", "623906f285c656a7c6e4f3cecd26bc5e2b6da38ba07f0cc76a235f7e524814af", main_image),
    ("NKLD0218.BIN", "bc26b7413adabeabbf21e341197bc14c58666b0be5f5a8c2eac29564c73f5f1d", lens_data),
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("main_image", type=Path)
    parser.add_argument("lens_data", type=Path)
    args = parser.parse_args()
    results = []
    for path, (name, expected, module) in zip((args.main_image, args.lens_data), SAMPLES):
        try:
            data = path.read_bytes()
        except OSError as error:
            parser.exit(1, f"{error}\n")
        digest = hashlib.sha256(data).hexdigest()
        if digest != expected:
            parser.exit(1, f"{path}: SHA-256 {digest}, expected {expected}\n")
        results.append(dict(sample=name, size=len(data), sha256=digest, analysis=module.inspect(data)))
    print(json.dumps(results, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
