# Verify a Punchy APK

Use the source and APK from the same release tag. The package name is **dev.jaycee.punchy**. The repository's current main branch may contain documentation or later work; a tag identifies the source intended for that specific release.

## Download from the project release

Open [Punchy 0.1.5](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.5) and download `Punchy-0.1.5-debug.apk`, `SHA256SUMS` and `release-info.json`. GitHub repository access does not itself certify an APK as safe.

In the download directory on Linux:

```sh
sha256sum -c SHA256SUMS
```

The APK should report `OK`. On another system, calculate its SHA-256 and compare the complete value to `SHA256SUMS`. This verifies matching bytes against that published checksum; an attacker controlling both could replace both, so it is not independent authentication or a malware scan.

## Inspect the signature

With Android SDK Build Tools installed:

```sh
apksigner verify --verbose --print-certs Punchy-0.1.5-debug.apk
```

The established signer certificate SHA-256 for Punchy 0.1.0–0.1.5 is:

```text
8d3baa023fbbb7cc704189a686d0b203745833369708dffc980722babfb3792e
```

The certificate identifies itself as **Android Debug**. Current APKs intentionally retain the existing development signing key so Android can update the original installation without erasing its settings. They are debuggable development builds, not production releases. A matching signature establishes key continuity, not a verified real-world identity, harmlessness or bank approval. No private signing key is published. A later production-signing migration would need a separately documented update/reinstall plan.

`release-info.json` records the APK SHA-256, size, package, version, source commit, development-build status and signer fingerprint. It is a maintainer-generated record, not an independently signed attestation. Build/test results are linked in the release notes.

## Inspect or build the source

Review the [manifest](../app/src/main/AndroidManifest.xml), [Accessibility configuration](../app/src/main/res/xml/ring_accessibility.xml), [ring service](../app/src/main/java/dev/jaycee/punchy/RingService.java), [privacy explanation](../PRIVACY.md) and [security policy](../SECURITY.md).

To build the tagged source with the toolchain listed in the README:

```sh
git clone https://github.com/justinebacurin1927/Punchy.git
cd Punchy
git checkout v0.1.5
./gradlew :app:assembleDebug :app:lintDebug
./scripts/test-core.sh
```

Configure the local Android SDK first, as described in the README. Your debug APK will normally use your own local debug key. No byte-for-byte reproducibility or matching developer signature is claimed for a local build.

## Ask a bank to review the warning

Provide the exact version, package, tag/source commit, APK checksum, signer fingerprint and the permission explanation. Do not send credentials or private bank/notification screenshots through public GitHub issues. Ask whether the bank has a developer review or exception process. A clean security scan, public source or matching signature cannot force a bank to approve the app.
