# OpenEXPEED — Nikon D90 Firmware Reverse Engineering

**OpenEXPEED** is an independent, open-source research project investigating the **Nikon D90 DSLR firmware**. It documents the camera's official firmware update packages, A/B firmware images, L lens-distortion data, and selected executable routines using reproducible binary analysis, disassembly, and bounded experiments.

The research covers early Nikon EXPEED-era firmware behavior, **Fujitsu FR instruction analysis**, memory mapping, boot and update paths, NOR flash programming, and A/B communication. The repository provides **Python analysis tools**, **Ghidra preparation scripts**, reference results, and evidence-linked technical documentation. The aim is to understand what the firmware does, assess opportunities for safe offline experimentation, and investigate the feasibility of future emulation—not to claim a working custom firmware.

> **Research alpha — not a camera firmware replacement.** Selected routines have been examined and exercised with synthetic inputs and simulated memory. There is **no complete Nikon D90 emulator, validated firmware patch, or verified hardware modification**. See [current research status](STATUS.md) for verified findings, open questions, and limitations.

## Why this project exists

Public documentation of Nikon DSLR firmware internals is scattered across past research, tools, and community discussions. OpenEXPEED aims to make **Nikon D90 firmware reverse engineering** more reproducible and easier to build upon: findings include sample identity, addresses or offsets, methods, observed behavior, and explicit uncertainty. Researchers can independently check results and contribute corrections or new evidence.

**Interested in contributing?** Help reproduce an analysis, review a Fujitsu FR disassembly, identify hardware interfaces, investigate Nikon update mechanisms, or share relevant prior work. Start with [CONTRIBUTING.md](CONTRIBUTING.md), the [technical documentation](docs/README.md), and [related projects](docs/related-projects.md).

## Main results so far

- The combined A/B update package has been separated into its A and B regions, with boundaries and per-region checksums tied to observed update code ([AB-11](docs/update-and-flash.md#ab-11)).
- The L sample decodes into 307 records under the documented recurrence; the record fields and trailing data are not fully understood, and no executable code has been established ([L-1](docs/firmware-formats.md#l-1), [L-2](docs/firmware-formats.md#l-2)).
- Selected B startup and NOR flash programming paths have been followed statically, with bounded instruction-level experiments against simulated memory ([AB-15–AB-18](docs/update-and-flash.md#ab-15)).
- Isolated experiments cover selected A/B communication handlers. AB-43–AB-46 trace an enable flag and the movement of list elements, while their camera-level meaning remains unresolved ([AB-30–AB-46](docs/communication-analysis.md#ab-30), [AB-43–AB-46](docs/communication-topology.md#ab-43)).

## Tools and materials

- A reproducible Python analyzer for the D90 A/B update package and the L data package.
- A reference JSON report with sample digests and decoded structural results.
- Ghidra preparation scripts and isolated FR instruction experiments for selected startup, update, flash, and communication paths.
- Technical notes that distinguish observed bytes and instruction behavior from hypotheses and unresolved questions.

The repository contains analysis and tooling, **not Nikon firmware files**. Obtain the official files separately and keep them outside the repository.

## Reproduce the package analysis

Use Python 3.8 or newer. From the repository root, run the analyzer against local copies of both firmware files:

```sh
python3 tools/analyze.py "/path/to/D90_0101.bin" "/path/to/NKLD0218.BIN" > /tmp/d90-report.json
diff -u reports/reference.json /tmp/d90-report.json
```

The analyzer opens the inputs read-only and checks their SHA-256 digests before decoding. A clean `diff` reproduces the checked-in report. The reference digests identify the analyzed samples; Nikon’s download pages do not publish an internal-file digest for independent authentication.

Run the synthetic parser and digest-gate tests with `python3 -m unittest discover -s tests -v`. They check NKLD parsing and malformed bounds without firmware files, so they do not independently verify sample-specific findings.

## Documentation

- [Documentation index](docs/README.md): topic map and stable finding IDs.
- [Firmware samples and package structure](docs/firmware-formats.md): sample identity, hashes, CRCs, A/B mapping, and L-data structure (AB-1–AB-5, L-1–L-2).
- [Update package and flash path](docs/update-and-flash.md): update selection, validation, flash operations, and associated display paths (AB-6–AB-23).
- [Boot analysis and Ghidra work](docs/boot-and-ghidra.md): B-image import and startup analysis (AB-24–AB-29).
- [Communication analysis](docs/communication-analysis.md): A/B transport paths and isolated experiments (AB-30–AB-42).
- [Communication topology and callback details](docs/communication-topology.md): external comparison and expanded evidence for AB-38–AB-46.
- [Related projects](docs/related-projects.md): external projects reviewed for relevance.

For the current research status, see [STATUS.md](STATUS.md). To reproduce Ghidra work or submit a finding, see [CONTRIBUTING.md](CONTRIBUTING.md).
