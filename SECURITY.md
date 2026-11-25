# Security policy

## Project status

Punchy is a personal, experimental Android utility. The current published version is 0.1.5; older versions are retained for history and are not maintained as separate branches. Fixes, when available, will be delivered in a newer version. There is no guaranteed support or response time.

Published APKs are **development-signed debug builds**, not production-certified releases. This repository, release checksums, source visibility and passing tests provide inspectable evidence. They do not constitute an independent security audit, malware-free guarantee, bank approval or endorsement by GitHub, Google, HONOR or MariBank. No independent security audit is claimed.

## Report a vulnerability privately

Use GitHub's [Report a vulnerability](https://github.com/justinebacurin1927/Punchy/security/advisories/new) form. Private vulnerability reporting is enabled for this repository. Include the affected Punchy version, Android version, reproduction steps and likely impact. Use synthetic examples and redact personal notification content, credentials and account information.

If that form is unavailable, open an issue asking for a private reporting route without including exploit details or private data. General bugs and feature requests may use [Issues](https://github.com/justinebacurin1927/Punchy/issues).

## Permissions and sensitive data

Read [Privacy and permissions](PRIVACY.md) before enabling either notification or Accessibility access. The source limits Accessibility capability to the ring overlay, but notification access can expose sensitive notification content. Source-app selection does not narrow Android's underlying notification grant.

The main screen and expanded island offer **Turn off Accessibility**, which disables Punchy's own Android Accessibility service. Reenable it manually in Android settings, then turn on Enable Punchy. Notification access is separate and stays enabled until revoked in Android settings. The regular display switch and Quick Settings tile do not revoke either grant.

## Banking-app warnings

Some banking apps block sideloaded apps with Accessibility enabled. A warning is important compatibility evidence; Punchy cannot determine or override a bank's trust decision. If your bank offers disabling Accessibility as a remedy, use that option and reopen the bank. If it still blocks access, follow the bank's instructions or contact its official support. Punchy does not guarantee this resolves every warning.

The app does not spoof its installer, hide its service, monitor banking screens or disable a bank's protection. See [APK verification](docs/VERIFY.md) for evidence you can supply when requesting a review. A bank may decline an exception even after inspecting it.
