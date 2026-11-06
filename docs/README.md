# Documentation index

The technical notes retain the original discovery identifiers. Use an ID such as `AB-17` or `L-2` as a stable cross-reference; each finding has a direct anchor in its document. The findings record static observations and bounded experiments, with hypotheses and limits stated alongside them.

## Findings by topic

| Topic | Findings | Document |
|---|---|---|
| Sample identity, checksums, package boundaries, and preliminary image map | AB-1–AB-5; L-1–L-2 | [Firmware samples and package structure](firmware-formats.md) |
| Update file selection, validation, flash programming, and display path | AB-6–AB-23 | [Update package and flash path](update-and-flash.md) |
| B image import, reset, startup tables, and ENDM initialization | AB-24–AB-29 | [Boot analysis and Ghidra work](boot-and-ghidra.md) |
| A/B communication paths and isolated routine experiments | AB-30–AB-42 | [Communication analysis](communication-analysis.md) |
| External transport comparison and expanded callback/hardware evidence | AB-38–AB-97 | [Communication topology](communication-topology.md) |
| External projects evaluated | — | [Related projects](related-projects.md) |

The expanded communication-topology document adds detail to findings AB-38–AB-97; it does not replace their overview in the communication analysis. `STATUS.md` tracks current priorities and open questions. `CONTRIBUTING.md` describes how to reproduce or submit work.
