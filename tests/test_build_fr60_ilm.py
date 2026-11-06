"""Builder validation with synthetic inputs; does not validate FR60 semantics."""
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

BUILDER = Path(__file__).resolve().parents[1] / 'tools/build_fr60_ilm.py'


class BuilderTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'tools').mkdir()
        (self.root / 'ghidra').mkdir()
        self.builder = self.root / 'tools/build_fr60_ilm.py'
        shutil.copyfile(BUILDER, self.builder)
        self.source = self.root / 'source'
        self.source.mkdir()
        self.inputs = {'fr60.sinc': b'PS[16,4] = ILM;\nILM = PS[16,4];\nILM = u8:1 & 0x0f;\n'}
        self.inputs.update({name: name.encode() for name in
                           ['fr60.slaspec', 'fr60.pspec', 'fr60.cspec', 'fr60.dwarf']})
        for name, data in self.inputs.items():
            (self.source / name).write_bytes(data)
        (self.root / 'ghidra/language-profile.sha256').write_text(''.join(
            hashlib.sha256(data).hexdigest() + '  ' + name + '\n'
            for name, data in self.inputs.items()))
        self.output = self.root / 'output'

    def run_builder(self):
        return subprocess.run([sys.executable, str(self.builder), str(self.source), str(self.output)],
                              capture_output=True, text=True)

    def test_valid_inputs_and_generated_manifest(self):
        result = self.run_builder()
        self.assertEqual(result.returncode, 0, result.stderr)
        for name, data in self.inputs.items():
            if name != 'fr60.sinc':
                self.assertEqual((self.output / name).read_bytes(), data)
        self.assertIn('PS[16,5]', (self.output / 'fr60.sinc').read_text())
        for manifest in ['inputs.sha256', 'generated.sha256']:
            directory = self.source if manifest == 'inputs.sha256' else self.output
            for line in (self.output / manifest).read_text().splitlines():
                digest, name = line.split()
                self.assertEqual(hashlib.sha256((directory / name).read_bytes()).hexdigest(), digest)
        before = (self.output / 'generated.sha256').read_bytes()
        self.assertNotEqual(self.run_builder().returncode, 0)
        self.assertEqual((self.output / 'generated.sha256').read_bytes(), before)

    def test_each_altered_input_rejected_before_output(self):
        for name, data in self.inputs.items():
            with self.subTest(name=name):
                (self.source / name).write_bytes(data + b'changed')
                result = self.run_builder()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn('Unreviewed ' + name, result.stderr)
                self.assertFalse(self.output.exists())
                (self.source / name).write_bytes(data)

    def test_missing_input_rejected_before_output(self):
        (self.source / 'fr60.dwarf').unlink()
        self.assertNotEqual(self.run_builder().returncode, 0)
        self.assertFalse(self.output.exists())

    def test_output_inside_source_rejected(self):
        self.output = self.source / 'nested'
        self.assertNotEqual(self.run_builder().returncode, 0)
        self.assertFalse(self.output.exists())


if __name__ == '__main__':
    unittest.main()
