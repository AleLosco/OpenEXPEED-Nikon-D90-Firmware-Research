import hashlib
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(TOOLS))

import lens_data  # noqa: E402


def encode_records(decoded):
    encoded = bytearray()
    key = 148
    delta = 223 * 0x260
    for value in decoded:
        key = (key + delta) & 255
        delta = (delta + 223) & 255
        encoded.append(value ^ key)
    return bytes(encoded)


def make_sample(records=(b"\x00\x04ab", b"\x00\x04cd"), corrupt_main=False,
                corrupt_suffix=False):
    decoded = b"".join(records)
    encrypted = encode_records(decoded)
    start = 32
    span = len(encrypted) + 2
    record_end = start + len(encrypted)
    suffix_start = start + span + 2
    sample = bytearray(suffix_start + 256)

    sample[0:2] = start.to_bytes(2, "big")
    sample[4] = 2
    sample[5] = 18
    sample[6:8] = len(records).to_bytes(2, "big")
    sample[8:12] = (0x87C7CAAC).to_bytes(4, "big")
    sample[12:16] = span.to_bytes(4, "big")
    sample[start:record_end] = encrypted
    sample[28:32] = suffix_start.to_bytes(4, "big")
    main_crc = lens_data.crc_hqx(sample[:record_end], 0)
    sample[record_end:record_end + 2] = (main_crc ^ int(corrupt_main)).to_bytes(2, "big")
    suffix_crc_end = suffix_start + 228
    suffix_crc = lens_data.crc_hqx(sample[suffix_start:suffix_crc_end], 0)
    sample[suffix_crc_end:suffix_crc_end + 2] = (suffix_crc ^ int(corrupt_suffix)).to_bytes(2, "big")
    return bytes(sample)


class LensDataTests(unittest.TestCase):
    def test_valid_synthetic_container(self):
        result = lens_data.inspect(make_sample())
        self.assertEqual(result["version"], "2.018")
        self.assertEqual(result["declared_record_count"], 2)
        self.assertEqual(result["decoded_record_lengths"], {4: 2})
        self.assertTrue(result["crc16_xmodem_main"]["matches"])
        self.assertTrue(result["crc16_xmodem_suffix"]["matches"])

    def test_truncated_header_and_suffix_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "header truncated"):
            lens_data.inspect(b"\0" * 31)
        with self.assertRaisesRegex(ValueError, "bounds"):
            lens_data.inspect(make_sample()[:-1])

    def test_inconsistent_declared_bounds_are_rejected(self):
        sample = bytearray(make_sample())
        sample[28:32] = (len(sample) - 255).to_bytes(4, "big")
        with self.assertRaisesRegex(ValueError, "bounds"):
            lens_data.inspect(sample)

    def test_record_length_beyond_region_is_rejected(self):
        sample = make_sample(records=(b"\x00\x0aabcdef",))
        with self.assertRaisesRegex(ValueError, "record exceeds declared region"):
            lens_data.inspect(sample)

    def test_record_count_must_fill_decoded_region(self):
        sample = bytearray(make_sample())
        sample[6:8] = (1).to_bytes(2, "big")
        with self.assertRaisesRegex(ValueError, "do not fill declared region"):
            lens_data.inspect(sample)

    def test_crc_mismatches_are_reported_without_rejecting_container(self):
        result = lens_data.inspect(make_sample(corrupt_main=True, corrupt_suffix=True))
        self.assertFalse(result["crc16_xmodem_main"]["matches"])
        self.assertFalse(result["crc16_xmodem_suffix"]["matches"])
        self.assertNotEqual(
            result["crc16_xmodem_main"]["stored"],
            result["crc16_xmodem_main"]["calculated"],
        )
        self.assertNotEqual(
            result["crc16_xmodem_suffix"]["stored"],
            result["crc16_xmodem_suffix"]["calculated"],
        )

    def test_cli_rejects_input_that_fails_sample_digest_gate(self):
        with tempfile.TemporaryDirectory() as directory:
            main_path = Path(directory) / "D90_0101.bin"
            lens_path = Path(directory) / "NKLD0218.BIN"
            main_path.write_bytes(b"synthetic main image")
            lens_path.write_bytes(make_sample())
            result = subprocess.run(
                [sys.executable, str(TOOLS / "analyze.py"), str(main_path), str(lens_path)],
                cwd=ROOT,
                capture_output=True,
                text=True,
                check=False,
            )
        observed_digest = hashlib.sha256(b"synthetic main image").hexdigest()
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, "")
        self.assertIn(observed_digest, result.stderr)
        self.assertIn("expected", result.stderr)


if __name__ == "__main__":
    unittest.main()
