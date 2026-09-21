# C10 foundation and C01 implementation ledger

## Approved ordering and scope

The user approved C10 foundation, then C01/C02/C03/C04/C05/C09, then C06/C07/C08, and only then C10 integration closure. `feature/2.0.x` is the canonical implementation line. This file records execution, not completion of the ten changes.

An isolated implementation branch starts at `d0c8056990f7a47fcc202acffa387ba066bcfc67`. The 1.x/3.x baseline refs and permitted JDK/Jackson/Maven differences are recorded in `src/test/resources/opencli-contracts/v1/sources.lock.json`. No changes to `main`, dependency versions or coverage thresholds are part of this increment.

## C10 foundation

Shared synthetic UTF-8/base64 argv vectors preserve empty and trailing empty fields. They have content hashes and a specification ref, not an invented upstream capture provenance. A Java 8-compatible child prints each actual argument. The regression suite uses the real SDK executor, adapter and Browser paths; a Recording executor is not used to prove process behavior.

The report runner separates enumeration, argv, protocol, typed-result and real-execution evidence. Only explicitly selected suites contribute; the real-execution layer remains NOT_RUN because a Java argv probe is not a live OpenCLI website test. Missing reports, zero tests, skipped required tests, nonzero Maven exit, malformed XML and inconsistent testcase counts fail closed.

Local runner TDD: 12 failures before the runner existed, then 12 passing tests. The probe compiled with `javac --release 8` on JDK 21 and emitted an empty token unchanged. This does not constitute an actual JDK 8 runtime test. Full JVM evidence is produced by the branch-specific GitHub Actions workflow using each line's checked-in Maven wrapper.

## C01 RED checkpoint

The initial Java contract suite deliberately demands lossless values before any product source is changed. The first CI run must be inspected for assertion failures at the real child boundary, not treated as a completed fix. Compilation errors or tool setup failures are not valid RED proof.

## Still open

C01 schema-aware repeated/false option handling, C02/C03/C04/C05/C09 production fixes, discovery, Browser result models, context/diagnostics and three-branch integration closure are not complete. Official OpenSpec strict is configured but must be observed at the exact workflow run before claiming it passed. No OpenSpec implementation tasks are pre-checked.
