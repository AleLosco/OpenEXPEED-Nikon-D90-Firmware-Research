# Nikon D90 Firmware Research

Independent, evidence-led research into the Nikon D90 firmware update packages, A/B firmware images, L lens-distortion data, and selected firmware routines. The goals are to document the package formats, map and understand firmware behavior, test carefully scoped changes offline, and assess whether useful hardware emulation is feasible.

**Project stage:** research alpha. Package structure, selected update and flash paths, startup behavior, and communication routines have been documented with bounded instruction-level experiments. A recent independent review led to two local reproducibility fixes: odd-PC rejection in the synthetic INT adapter and digest checks for every input to the experimental FR60 language builder. A complete camera emulator, full peripheral model, and validated firmware patch have not been established.

**Estimated research progress: about 45%.** This is a roadmap estimate based on the breadth of the remaining goals, not a measure of firmware instructions understood. Package analysis is substantially mature; camera-wide behavior, realistic emulation, and patches remain early or unstarted. See [STATUS.md](STATUS.md) for area estimates, current evidence, and open work.

## Main results so far

- The combined A/B update package has been separated into its A and B regions, with boundaries and per-region checksums tied to observed update code ([AB-11](docs/update-and-flash.md#ab-11)).
- The L sample decodes into 307 records under the documented recurrence; the record fields and trailing data are not fully understood, and no executable code has been established ([L-1](docs/firmware-formats.md#l-1), [L-2](docs/firmware-formats.md#l-2)).
- Selected B startup and NOR flash programming paths have been followed statically, with bounded instruction-level experiments against simulated memory ([AB-15–AB-18](docs/update-and-flash.md#ab-15)).
- Isolated experiments cover selected A/B communication handlers. B-side findings AB-43–AB-97 trace selected state, callback, buffer and ATAM paths. Recent work follows selector 0x22 through the interrupt vector, corrects the AB-70 state-zero case, maps accesses to the status register, and reaches the OS service -85 using synthetic frames and confirms a bounded linked-list removal operation. Later experiments cover synthetic OS queue operations and register frames, and characterize interrupt-emulation limits, including an isolated five-bit ILM correction. The AB-86 adapter now rejects odd PCs before changing state; its six negative cases verify registers and stack bytes remain unchanged. The AB-85 builder verifies all five copied source files and records input and generated artifact digests. Findings through [AB-117](docs/communication-topology.md#ab-117--selezione-delimitata-del-candidato-sullo-stato-inizializzato) connect OS initialization to an initial user context, then trace a bounded candidate-selection path from the ready bitmap. The experiments retain explicit INT, stack-switch, RETI, and SRCH1 adapters; the vector entry is imposed by the harness, and the path stops before context save. The native FR81 SRCH1 probe remains blocked by an unimplemented `clz` p-code operation. Hardware interrupt acceptance, timing, task scheduling, full camera emulation, and a validated patch remain open ([AB-30–AB-42](docs/communication-analysis.md#ab-30), [AB-43–AB-117](docs/communication-topology.md#ab-43--due-produttori-del-byte-di-abilitazione)).

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

Run the synthetic parser and digest-gate tests with `python3 -m unittest discover -s tests -v`. They check NKLD container parsing and malformed bounds without requiring firmware files. They do not independently verify claims tied to the official samples.

## Documentation

- [Documentation index](docs/README.md): topic map and stable finding IDs.
- [Firmware samples and package structure](docs/firmware-formats.md): sample identity, hashes, CRCs, A/B mapping, and L-data structure (AB-1–AB-5, L-1–L-2).
- [Update package and flash path](docs/update-and-flash.md): update selection, validation, flash operations, and associated display paths (AB-6–AB-23).
- [Boot analysis and Ghidra work](docs/boot-and-ghidra.md): B-image import and startup analysis (AB-24–AB-29).
- [Communication analysis](docs/communication-analysis.md): A/B transport paths and isolated experiments (AB-30–AB-42).
- [Communication topology and callback details](docs/communication-topology.md): external comparison and expanded evidence for AB-38–AB-105.
- [Related projects](docs/related-projects.md): external projects reviewed for relevance.

For the current research status, see [STATUS.md](STATUS.md). To reproduce Ghidra work or submit a finding, see [CONTRIBUTING.md](CONTRIBUTING.md).
