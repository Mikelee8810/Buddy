# Building Scribe

## Easiest: let GitHub build it

Every push to a pull request, every push to `main`, and every `v*` tag runs the **Build Scribe APK** workflow. When it finishes, open the run on the **Actions** tab and download the **Scribe** artifact. It contains `Scribe-<version>.apk`. Tags also attach the APK to a GitHub Release.

You can also start a build by hand: **Actions → Build Scribe APK → Run workflow**.

## On your own computer

Requirements:

- Android Studio (latest stable) or the Android command-line tools
- JDK 17
- Android SDK platform 37

Steps:

1. Clone the repository and open it in Android Studio.
2. Wait for Gradle sync to finish.
3. Click **Run** to install on a connected phone, or build an APK from the command line:

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`.

## Versions

| Tool | Version |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Gradle | 9.8.2 |
| Kotlin (Compose compiler plugin) | 2.2.10 |
| compileSdk / targetSdk / minSdk | 37 / 35 / 26 |
| Compose BOM / Material 3 | 2026.09.00 / 1.5.0-rc01 |

## Signing

Release builds are signed with the default debug key. GitHub Actions keeps one key in its build cache so that consecutive builds can update each other. A build made on a different computer uses that computer's key, so Android will not install it over a copy built elsewhere.
