# meta-je-example-bsp

A worked example of [meta-justembed-security](https://github.com/justembed-labs/meta-justembed-security)'s four layers, built on QEMU so anyone can reproduce it with no physical hardware.

```
kas build kas.yml
```

builds `core-image-minimal` for `qemuarm64` with `meta-je-hygiene`, `meta-je-sbom-cve`, `meta-je-detection`, and `meta-je-boot-update` all wired in exactly as the parent project's README describes -- the real kas dependency, the real kernel config fragment (`sources/meta-je-example/recipes-kernel/linux/`), the real `IMAGE_CLASSES`/`INHERIT` lines.

## Live evidence

**[View the real SBOM/CVE evidence from this build →](https://justembed-labs.github.io/meta-je-example-bsp/)**

Three real scan runs, published as-is:

- A baseline scan.
- A second run with a real scan-to-scan diff against the first (0 drift -- the diff mechanism correctly reporting no change between two identical builds).
- A third run with KEV/EPSS enrichment enabled, showing real cross-reference against CISA's Known Exploited Vulnerabilities catalog and FIRST.org's EPSS scores -- including a real CPE-mismatch case (`CVE-2023-3079`, a Chrome/V8 CVE, confirmed exploited-in-the-wild by KEV but landing on the Linux kernel's CPE) already documented in the parent project.

## What this proves

- The parent repo's own "Getting started" instructions work verbatim for a fresh adopter -- this project was built by literally following them against the real public repo URL, not a local checkout.
- `je-hygiene`'s enforcement is real: this exact build fails outright if `JE_HYGIENE_REQUIRE_NO_DEFAULT_CREDS` isn't explicitly turned off, because `core-image-minimal` ships a blank root password by default.
- `je-sbom-cve`'s scan, diff, and KEV/EPSS triage all run as real bitbake tasks producing real output, not mocked.
- `je-detection` and `je-boot-update`'s packages install cleanly alongside a completely different package set than the parent project's own reference hardware.

## Not covered here

Secure boot (`je-secureboot`) and the FOTA mechanism (`je-swupdate-fota`) are wired into this build's image classes, but a full signed-update-and-reboot cycle needs a real partition layout and `sw-description` -- board-specific integration work that isn't part of this example yet.
