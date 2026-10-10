# Contributing

JDK 17 / SDK 35. Run inventory validator, source-policy checks, `./gradlew testDebugUnitTest lintDebug assembleDebug`, and instrumented tests on API 26+ when available. Never claim Samsung device acceptance from a compile. Record commands and actual outcomes in TEST_REPORT.md.

Core logic must be immutable, bounded, deterministic where appropriate, and testable without Android. Use the shared Room library and the single explicit-destination writer. Preserve Home-only safety tests. Do not introduce polling services, destructive migrations, empty interfaces, nonfunctional toggles, untrusted code loading, unnecessary permissions, analytics, or a required backend.

Keep raw artwork and user state out of Git. No credentials, signing keys, local.properties, caches, APKs, or databases in source commits. Rendered/generated APKs belong in Actions artifacts. Document dependencies and optional permissions. Supply evidence before assigning DONE_VERIFIED. Use `tools/feature_overrides.json`, regenerate both matrices, and run the validator.

Use cohesive reviewed commits and ordinary fast-forward pushes. Do not force-push or alter unrelated history. Production signing/publishing requires explicit authorization and an external signing configuration.
