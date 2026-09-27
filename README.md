# meta-je-example-bsp

Runnable QEMU reference for
[`meta-justembed-security`](https://github.com/justembed-labs/meta-justembed-security).

**No hardware required.**

**[Build](#build) · [Boot](#boot) · [Evidence](#evidence)**

## What this is

A worked example of all four layers (Hygiene, Evidence, Detect,
Update & Boot) built into a real `core-image-minimal` for
`qemuarm64` -- built by literally following the parent project's own
"Getting started" instructions against the real public repo URL, not
a local checkout.

## Requirements

`kas`, and whatever `kas build` itself needs (bitbake's usual host
dependencies). No physical hardware, no board, no flashing.

## Build

```bash
git clone https://github.com/justembed-labs/meta-je-example-bsp.git
cd meta-je-example-bsp
kas build kas.yml
```

## Boot

```bash
kas shell kas.yml -c "runqemu qemuarm64 nographic"
```

To exercise the real U-Boot-mediated secure-boot chain instead of
`runqemu`'s dev loop, see [`docs/secure-boot.md`](docs/secure-boot.md)
-- a few extra commands, not a different build.

## What you can evaluate

- **Layer integration** -- all four layers wired into one real image
  with a completely different package set than the parent project's
  own reference hardware.
- **`je-hygiene` enforcement** -- this exact build fails outright if
  `JE_HYGIENE_REQUIRE_NO_DEFAULT_CREDS` isn't explicitly turned off,
  because `core-image-minimal` ships a blank root password by default.
- **SBOM/CVE output, diff, and KEV/EPSS triage** -- real bitbake
  tasks, real output, not mocked.
- **Detection agent and OCSF output** -- `je-detection-agent` runs as
  a real systemd service on the booted system, emitting OCSF Security
  Finding events.
- **Signed FIT verification and tamper rejection** -- a real
  signature check, real boot to a login prompt, and a real
  tamper-rejection negative case.
- **Signed FOTA and A/B rollback** -- a full update cycle proven both
  ways (committed update stays on the new copy; an unconfirmed update
  reverts), confirmed against real disk state.

## Evidence

**[View the real SBOM/CVE evidence from this build →](https://justembed-labs.github.io/meta-je-example-bsp/)**

Three real scan runs, published as-is: a baseline, a second run with a
real scan-to-scan diff (0 drift), and a third with KEV/EPSS enrichment
enabled (including a real CPE-mismatch case). Build your own by
following `meta-je-sbom-cve`'s `do_je_cve_diff` task against this same
`kas.yml`.

Detection and update/boot evidence for this exact reference
implementation lives in the parent repo:
[`docs/evidence/detection-event.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/evidence/detection-event.md),
[`docs/evidence/fit-verification.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/evidence/fit-verification.md),
[`docs/evidence/signed-ab-update.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/evidence/signed-ab-update.md).

## Limitations

- QEMU's trust anchor is not an immutable SoC hardware root of trust
  -- the verified chain here starts at U-Boot, not at a boot ROM. See
  [`docs/secure-boot.md`](docs/secure-boot.md).
- Hardware-specific assurance requires validation on the actual
  target -- this reference demonstrates the mechanisms correctly, it
  does not substitute for testing on a real product's own SoC.
- Booting through the real secure-boot/FOTA chain needs a handful of
  commands typed by hand at the U-Boot console (QEMU's generic `virt`
  machine has no compiled-in boot script to do this automatically, the
  way a real board would) -- see
  [`docs/secure-boot.md`](docs/secure-boot.md) and
  [`docs/fota.md`](docs/fota.md) for the exact sequence. `runqemu`'s
  default dev loop (above) avoids this entirely but also bypasses
  U-Boot, so it can't demonstrate the secure-boot/FOTA proofs.
## License

Apache-2.0. See [LICENSE](LICENSE).

## Deep dives

- [`docs/secure-boot.md`](docs/secure-boot.md) -- exact console
  output, the real problems fixed to get FIT verification working.
- [`docs/fota.md`](docs/fota.md) -- exact console output, the real
  problems fixed to get signed A/B updates and rollback working.
- [`meta-justembed-security`](https://github.com/justembed-labs/meta-justembed-security)
  -- the reusable layer set this repo is a worked example of; this
  repo only adds the QEMU-specific glue (devicetree capture,
  `u-boot.bbappend`, `kas.yml`) needed to run it.
