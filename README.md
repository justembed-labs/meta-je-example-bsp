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

## Secure boot, verified live

`je-secureboot`'s FIT signature verification runs for real on this build --
not just "the mechanism is wired in", but a live U-Boot instance checking
a real signature and rejecting a tampered image.

`runqemu`'s default dev-loop boots the kernel directly (`-kernel fitImage`
straight to `qemu-system-aarch64`), which bypasses U-Boot entirely. To
exercise the real boot chain, U-Boot itself has to be QEMU's bootloader:

```
qemu-system-aarch64 -machine virt -cpu cortex-a57 -smp 1 -m 512 -nographic \
    -bios build/tmp/deploy/images/qemuarm64/u-boot.bin \
    -kernel build/tmp/deploy/images/qemuarm64/fitImage \
    -serial mon:stdio
```

At the `=>` prompt, reload the kernel cleanly via QEMU's fw_cfg device and
boot it, naming the FIT configuration explicitly (`u-boot`'s
`CONFIG_FIT_BEST_MATCH` otherwise looks for a `compatible` match against
the board's own devicetree, which this configuration doesn't set):

```
=> qfw load 40400000 44000000
=> bootm 40400000#conf-1
...
   Verifying Hash Integrity ... OK
   ...
   Verifying Hash Integrity ... sha256+ OK
```

The `+` marks a signature-backed verification, not a bare hash check --
this is `je-secureboot`'s RSA key (`je-secureboot-keys/je-secureboot-dev`)
actually being checked against the public key `uboot-sign.bbclass`
embedded into `u-boot.dtb` at build time.

Flipping one byte in the kernel payload and repeating the same boot:

```
   Verifying Hash Integrity ... sha256 error!
Bad hash value for 'hash-1' hash node in 'kernel-1' image node
Bad Data Hash
ERROR: can't get kernel image!
```

U-Boot refuses to boot it. This is the actual negative case, not an
assumption about what the mechanism *should* do.

What made this non-trivial: `je-secureboot.bbclass` deliberately doesn't
(and can't) inherit `uboot-sign.bbclass` on the u-boot recipe or touch its
`.config` -- that's the consuming BSP's job, done here in
`sources/meta-je-example/recipes-bsp/u-boot/u-boot_%.bbappend`. Getting it
working on `qemu_arm64_defconfig` needed one non-obvious fix:
`UBOOT_DTB_BINARY` has to be a flat filename copied into place after
`do_compile` (`arch/arm/dts/qemu-arm64.dtb` on this board), not the nested
path where the real dtb lives -- `uboot-sign.bbclass` uses the same
variable both as a build-relative path and a bare deploy filename.

## Not covered here

The FOTA mechanism (`je-swupdate-fota`) is wired into this build's image
classes, but a full signed-update-and-reboot cycle needs a real partition
layout and `sw-description` -- board-specific integration work that isn't
part of this example yet.
