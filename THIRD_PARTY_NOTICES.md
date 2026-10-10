# Third-party dependencies and notices

DEX Wallpaper source: Apache License 2.0 (see LICENSE). No third-party artwork, proprietary wallpaper runtime, remote model, custom font, or advertising SDK is bundled. System fonts are provided by Android.

| Dependency | Pinned version | License / source |
|---|---|---|
| Android Gradle Plugin | 8.9.2 | Apache-2.0; Android tools |
| Gradle / authentic wrapper | 8.11.1 | Apache-2.0; gradle/gradle v8.11.1 |
| Kotlin / Compose compiler plugin | 2.1.20 | Apache-2.0; JetBrains/kotlin |
| Kotlin coroutines | 1.10.2 | Apache-2.0; Kotlin/kotlinx.coroutines |
| Compose UI / Material 3 / Material icons | BOM 2025.04.01 | Apache-2.0; AndroidX / Material Design icons |
| AndroidX Core | 1.16.0 | Apache-2.0 |
| AndroidX Activity | 1.10.1 | Apache-2.0 |
| AndroidX Lifecycle | 2.9.0 | Apache-2.0 |
| AndroidX Room | 2.7.1 | Apache-2.0 |
| AndroidX DataStore | 1.1.4 | Apache-2.0 |
| AndroidX WorkManager | 2.10.1 | Apache-2.0 |
| AndroidX DocumentFile | 1.0.1 | Apache-2.0 |
| AndroidX ExifInterface | 1.4.1 | Apache-2.0 |
| AndroidX Palette | 1.0.0 | Apache-2.0 |
| JUnit 4 (tests) | 4.13.2 | Eclipse Public License 1.0 |
| AndroidX Test / Espresso (tests) | ext 1.2.1, runner 1.6.2, Espresso 3.6.1 | Apache-2.0 |

AndroidX project: https://android.googlesource.com/platform/frameworks/support/ ; licenses are retained by dependency packaging. Gradle repositories are Google Maven, Maven Central, and Gradle Plugin Portal. CI pins official checkout/setup-java/upload-artifact, gradle/actions, and android-actions/setup-android by verified GitHub commit IDs. GitHub-hosted tools are build-time dependencies, not runtime artwork providers.

The Gradle wrapper and distribution checksums were read from the official v8.11.1 GitHub release. Imported artwork license/attribution is supplied by its owner and retained as metadata; DEX grants no rights to redistribute it. Provider integrations and proprietary imports require separate license/terms review. This report is not a claim that optional unimplemented integrations are licensed or available.
