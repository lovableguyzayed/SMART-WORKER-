# Self-hosted OTA updates

The app checks a hosted `update.json` on launch, and if a newer build is
available it downloads and installs the APK in-app — no Play Store, no manual
uninstall.

## One-time setup (release signing)

Every release **must** be signed with the same key, or Android refuses to update
in place. Generate the keystore once:

```bash
keytool -genkeypair -v -keystore release.jks -alias smartworker \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then copy `keystore.properties.template` → `keystore.properties` (repo root) and
fill it in:

```properties
storeFile=release.jks
storePassword=your-store-password
keyAlias=smartworker
keyPassword=your-key-password
```

`keystore.properties`, `*.jks`, and `*.keystore` are already git-ignored — never
commit them. When present, this key signs **both** debug and release builds, so
debug ↔ release installs on your own device also never need an uninstall.

## `update.json` fields

| Field | Meaning |
|---|---|
| `latestVersionCode` | Newest build's `versionCode`. The app updates when this is greater than the installed `versionCode`. |
| `latestVersionName` | Human-readable version shown in the dialog title. |
| `apkUrl` | Direct HTTPS link to the signed APK (GitHub Releases asset, your Flask backend, S3…). |
| `releaseNotes` | Shown in the dialog body (`\n` for line breaks). |
| `forceUpdate` | `true` = non-dismissable dialog, no "Later". |
| `minSupportedVersionCode` | Builds older than this are force-updated regardless of `forceUpdate`. |

The manifest URL lives in `UpdateViewModel.manifestUrl` — edit it to your host.

## How to ship a new version

1. **Bump** `versionCode` (increment by 1) **and** `versionName` in `app/build.gradle.kts`.
2. `./gradlew assembleRelease` — produces an APK signed with `release.jks`.
3. **Upload** `app/build/outputs/apk/release/app-release.apk` to your host
   (GitHub Releases or your Flask backend).
4. **Update** `update.json` — set `latestVersionCode`, `latestVersionName`,
   `apkUrl`, `releaseNotes` — and publish it at the manifest URL.
5. Done. Existing installs prompt to update on next launch.

> `applicationId` must **never** change once shipped — changing it makes Android
> treat the APK as a different app (side-by-side install, no update).

## One-time migration note

The **first** install of the newly-signed APK over an old, differently-signed
build requires **one** manual uninstall (signature change). Every install after
that — as long as the key and `applicationId` stay the same and `versionCode`
keeps increasing — is seamless.

## Compose version note

`UpdateDialog` uses the current lambda form
`LinearProgressIndicator(progress = { value })`. If you downgrade to a Compose
version older than 1.6 / Material3 1.2, switch to the deprecated float form
`LinearProgressIndicator(progress = value)`.
