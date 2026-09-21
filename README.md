# meta-je-example-bsp

**Runnable QEMU reference for
[meta-justembed-security](https://github.com/justembed-labs/meta-justembed-security).**

**No hardware required.**

A worked example of all four layers (Hygiene, Evidence, Detect, Update
& Boot) built into a real `core-image-minimal` for `qemuarm64` --
built by literally following the parent project's own "Getting
started" instructions against the real public repo URL, not a local
checkout.

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

## Generate / inspect evidence

**[View the real SBOM/CVE evidence from this build →](https://justembed-labs.github.io/meta-je-example-bsp/)**

Three real scan runs, published as-is: a baseline, a second run with a
real scan-to-scan diff (0 drift), and a third with KEV/EPSS enrichment
enabled (including a real CPE-mismatch case). Build your own by
following `meta-je-sbom-cve`'s `do_je_cve_diff` task against this same
`kas.yml`.

## What you can evaluate

- **Layer integration** -- all four layers wired into one real image
  with a completely different package set than the parent project's
  own reference hardware.
- **`je-hygiene` enforcement** -- this exact build fails outright if
  `JE_HYGIENE_REQUIRE_NO_DEFAULT_CREDS` isn't explicitly turned off,
  because `core-image-minimal` ships a blank root password by default.
- **SBOM/CVE output, diff, and KEV/EPSS triage** -- real bitbake
  tasks, real output, not mocked. See "Generate / inspect evidence"
  above.
- **Detection flow** -- `je-detection-agent` runs as a real systemd
  service on the booted system; see
  [`docs/detect.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/detect.md)
  and
  [`docs/evidence/detection-event.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/evidence/detection-event.md)
  in the parent repo for a real trigger through to a real event.
- **FIT verification** -- a real signature check, real boot to a login
  prompt, and a real tamper-rejection negative case. Full detail:
  [`docs/secure-boot.md`](docs/secure-boot.md).
- **Signed A/B updates** -- a full update cycle proven both ways
  (committed update stays on the new copy; an unconfirmed update
  reverts), confirmed against real disk state. Full detail:
  [`docs/fota.md`](docs/fota.md).

## Known friction

Booting through the real secure-boot/FOTA chain needs a handful of
commands typed by hand at the U-Boot console (QEMU's generic `virt`
machine has no compiled-in boot script to do this automatically, the
way a real board would) -- see
[`docs/secure-boot.md`](docs/secure-boot.md) and
[`docs/fota.md`](docs/fota.md) for the exact sequence. `runqemu`'s
default dev loop (above) avoids this entirely but also bypasses U-Boot,
so it can't demonstrate the secure-boot/FOTA proofs.

## Built on

This is a worked example, not a separate implementation --
[`meta-justembed-security`](https://github.com/justembed-labs/meta-justembed-security)
is the reusable layer set; this repo only adds the QEMU-specific glue
(devicetree capture, `u-boot.bbappend`, `kas.yml`) needed to run it.

## License

No `LICENSE` file is published in this repository yet -- see the
parent project's [`LICENSE`](https://github.com/justembed-labs/meta-justembed-security/blob/main/LICENSE)/[`NOTICE`](https://github.com/justembed-labs/meta-justembed-security/blob/main/NOTICE)
(Apache-2.0) in the meantime.
