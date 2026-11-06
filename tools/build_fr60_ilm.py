#!/usr/bin/env python3
"""Prepare an isolated five-bit ILM language; never change the installed processor."""
import argparse
import hashlib
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('language_dir', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    manifest = Path(__file__).resolve().parents[1] / 'ghidra/language-profile.sha256'
    expected = dict((name, digest) for digest, name in
                    (line.split() for line in manifest.read_text().splitlines() if line.strip()))
    names = ['fr60.sinc', 'fr60.slaspec', 'fr60.pspec', 'fr60.cspec', 'fr60.dwarf']
    # Validate every input before creating output; write these same validated bytes.
    inputs = {}
    for name in names:
        data = (args.language_dir / name).read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        if digest != expected.get(name):
            raise SystemExit('Unreviewed ' + name + ' hash: ' + digest)
        inputs[name] = data
    source = inputs['fr60.sinc']
    text = source.decode()
    replacements = [('PS[16,4] = ILM;', 'PS[16,5] = ILM;'),
                    ('ILM = PS[16,4];', 'ILM = PS[16,5];'),
                    ('ILM = u8:1 & 0x0f;', 'ILM = u8:1 & 0x1f;')]
    for old, new in replacements:
        if text.count(old) != 1:
            raise SystemExit('Unexpected replacement count: ' + old)
        text = text.replace(old, new)
    # Refuse an existing output, including an output inside the installed language tree.
    output = args.output.resolve()
    installed = args.language_dir.resolve()
    if output == installed or installed in output.parents:
        raise SystemExit('Output must be outside installed language directory')
    output.mkdir(parents=True, exist_ok=False)
    for name in ['fr60.slaspec', 'fr60.pspec', 'fr60.cspec', 'fr60.dwarf']:
        (output / name).write_bytes(inputs[name])
    (output / 'fr60.sinc').write_text(text)
    (output / 'fr60.ldefs').write_text('''<?xml version="1.0"?>
<language_definitions>
 <language processor="FR60" endian="big" size="32" variant="d90-ilm5" version="1.0"
 slafile="fr60.sla" processorspec="fr60.pspec" id="fr60:BE:16:d90-ilm5">
 <description>Experimental D90 ILM five-bit correction only</description>
 <compiler name="fcc911" spec="fr60.cspec" id="fcc911"/>
 </language>
</language_definitions>
''')
    (output / 'inputs.sha256').write_text(''.join(
        hashlib.sha256(inputs[name]).hexdigest() + '  ' + name + '\n'
        for name in sorted(inputs)))
    artifacts = sorted(output.glob('fr60.*'))
    (output / 'generated.sha256').write_text(''.join(
        hashlib.sha256(path.read_bytes()).hexdigest() + '  ' + path.name + '\n'
        for path in artifacts))
    print('Prepared isolated ILM correction:', output)
    print('Compile fr60.slaspec with the matching Ghidra support/sleigh')


if __name__ == '__main__':
    main()
