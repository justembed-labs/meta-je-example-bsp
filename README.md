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

## Secure boot, verified live -- all the way to a login prompt

`je-secureboot`'s FIT signature verification runs for real on this build,
and the result boots all the way to a real Linux login prompt -- not just
"the mechanism is wired in", but a live U-Boot instance checking a real
signature, booting the verified kernel, and landing at:

```
Poky (Yocto Project Reference Distro) 5.0.20 qemuarm64 ttyAMA0

qemuarm64 login:
```

with `je-detection-agent` running as a real systemd service on that booted
system.

`runqemu`'s default dev-loop boots the kernel directly (`-kernel fitImage`
straight to `qemu-system-aarch64`), which bypasses U-Boot entirely. To
exercise the real boot chain, U-Boot itself has to be QEMU's bootloader,
with the built rootfs attached as a virtio-blk disk:

```
qemu-system-aarch64 -machine virt -cpu cortex-a57 -smp 1 -m 512 -nographic \
    -bios build/tmp/deploy/images/qemuarm64/u-boot.bin \
    -kernel build/tmp/deploy/images/qemuarm64/fitImage \
    -drive file=build/tmp/deploy/images/qemuarm64/core-image-minimal-qemuarm64.rootfs.ext4,if=none,format=raw,id=hd0 \
    -device virtio-blk-device,drive=hd0 \
    -serial mon:stdio
```

At the `=>` prompt, set boot arguments, reload the kernel cleanly via
QEMU's fw_cfg device, and boot it, naming the FIT configuration explicitly
(`u-boot`'s `CONFIG_FIT_BEST_MATCH` otherwise looks for a `compatible`
match against the board's own devicetree, which a bare kernel-only config
doesn't set):

```
=> setenv bootargs root=/dev/vda rw console=ttyAMA0,115200 rootwait swiotlb=0
=> qfw load 40400000 44000000
=> bootm 40400000#conf-qemu-virt.dtb
...
   Verifying Hash Integrity ... OK
   ...
   Verifying Hash Integrity ... sha256+ OK
...
Starting kernel ...
...
Poky (Yocto Project Reference Distro) 5.0.20 qemuarm64 ttyAMA0
qemuarm64 login:
```

The `+` marks a signature-backed verification, not a bare hash check --
this is `je-secureboot`'s RSA key (`je-secureboot-keys/je-secureboot-dev`)
actually being checked against the public key `uboot-sign.bbclass`
embedded into `u-boot.dtb` at build time, for both the kernel and its
devicetree.

Flipping one byte in the kernel payload and repeating the same boot:

```
   Verifying Hash Integrity ... sha256 error!
Bad hash value for 'hash-1' hash node in 'kernel-1' image node
Bad Data Hash
ERROR: can't get kernel image!
```

U-Boot refuses to boot it. This is the actual negative case, not an
assumption about what the mechanism *should* do.

### What made this non-trivial

Three real, distinct problems, each fixed in this repo rather than worked
around:

- **The verifying key wasn't embedded.** `je-secureboot.bbclass`
  deliberately doesn't (and can't) inherit `uboot-sign.bbclass` on the
  u-boot recipe or touch its `.config` -- that's the consuming BSP's job,
  done here in `sources/meta-je-example/recipes-bsp/u-boot/u-boot_%.bbappend`.
  `qemu_arm64_defconfig` also needed one non-obvious fix: `UBOOT_DTB_BINARY`
  has to be a flat filename copied into place after `do_compile`
  (`arch/arm/dts/qemu-arm64.dtb` on this board), not the nested path where
  the real dtb lives -- `uboot-sign.bbclass` uses the same variable both
  as a build-relative path and a bare deploy filename.
- **No devicetree was reaching the kernel at all.** Unlike a real board,
  qemuarm64's `virt` machine has no static in-tree devicetree source --
  QEMU generates one dynamically per invocation. Without it, Linux
  couldn't even find a console and just hung after decompression.
  `sources/meta-je-example/recipes-bsp/devicetree/` captures that
  generated tree once and provides it through the standard `virtual/dtb`
  mechanism, so `kernel-fitimage.bbclass`'s real, unmodified pipeline
  embeds and signs it exactly like it would a real board's dts.
- **A hex-vs-decimal typo, and a phantom device.** `UBOOT_LOADADDRESS`
  needs an explicit `0x` prefix -- `dtc` reads a bare numeral in a FIT's
  `<...>` cell as decimal, so a naive `"48000000"` silently became decimal
  48,000,000 instead of the intended address, and U-Boot hung decompressing
  the kernel into the wrong memory. Separately, a devicetree captured
  *without* `-bios` still describes a PL061 GPIO controller that QEMU only
  creates when no firmware/bootloader is present -- booting through U-Boot
  with that devicetree made the kernel probe a bus address with nothing
  behind it and panic. Both are fixed in `kas.yml` and the devicetree
  capture; see the recipe's comments for the exact failure signatures.

## FOTA, verified live -- a full signed update, applied, booted, and rolled back

`je-swupdate-fota` is wired into this build too, with a real dual-copy
(A/B) partition layout, a real signed `.swu` bundle, and a full update
cycle proven end to end on QEMU -- including the negative case, which is
the part that actually matters for an atomic-update design.

The disk (`core-image-minimal-qemuarm64.rootfs.wic`, built by adding
`wic` to `IMAGE_FSTYPES`) carries four partitions: a small `boot`
partition holding two constant marker files, two full rootfs copies
(`rootA`, `rootB`), and a small `state` partition for the active
partition number and trial-boot flag. `bitbake -c swuimage update-image`
builds a signed `.swu` containing the new rootfs plus two activation
scripts (one per target copy).

Applying it for real:

```
/sbin/je-swupdate-select /path/to/update.swu
```

reads the running system's own `root=` from `/proc/cmdline` to pick the
*other* copy, then runs `swupdate -i ... -e stable,copyN -k
/etc/swupdate.pem -m` -- real RSA signature verification, a real raw
write to the inactive partition, then a real shellscript writing
`rootpart=N` / `bootstate=trial` to the state partition:

```
[INFO ] : SWUPDATE running :  Installation in progress
[INFO ] : SWUPDATE successful ! SWUPDATE successful !
```

On the next boot, U-Boot reads that state, sees `bootstate=trial`, and
arms a revert by writing a `bootstate=trying` marker before booting the
new copy -- exactly the trial-boot pattern a real board's own compiled-in
boot script would run, driven here by hand at the U-Boot prompt since
QEMU's generic `virt` machine has no such script to begin with:

```
=> printenv rootpart bootstate
rootpart=3
bootstate=trial
=> load virtio 0:1 44000000 bootstate-trying.env
=> fatwrite virtio 0:4 44000000 bootstate $filesize
=> setenv bootstate trying
```

**If the system boots and confirms itself healthy** (`je-swupdate-commit`
running once multi-user boot is reached), the next U-Boot check reads
`bootstate=good` and boots the new copy again, unconditionally --
confirmed by mounting the exact same, still-new rootfs partition a boot
later.

**If it never confirms** -- crash, panic, power loss, tested here by
simply killing the VM right after arming the trial, before any commit
could run -- the next U-Boot check reads `bootstate=trying` (never
cleared), reverts `rootpart` to the *other* copy, and boots what was
running before the update:

```
=> printenv rootpart bootstate
rootpart=3
bootstate=trying
=> if test "${rootpart}" = "2"; then setenv rootpart 3; else setenv rootpart 2; fi
=> fatwrite virtio 0:4 44000000 bootstate $filesize
=> setenv bootstate good
=> printenv rootpart bootstate
rootpart=2
bootstate=good
```

confirmed by the kernel mounting the original rootfs partition's own
filesystem UUID afterward -- not inferred from log lines, the actual
disk state.

### What made this non-trivial

- **`je-swupdate-select` hardcoded `/dev/vda`.** QEMU doesn't guarantee
  that letter -- attaching a second virtio-blk disk (even just to carry
  the `.swu` file into the guest for testing) can enumerate this disk as
  `vdb` instead. Fixed to match any `vd[a-z]` letter, the same reasoning
  the real hardware version already applies across `mmcblkN` controllers.
- **Missing `/etc/hwrevision`.** swupdate refuses to install anything
  without it (`HW compatibility not found`) -- `je-swupdate-conf`
  installs it.
- **swupdate's own bootloader-env bookkeeping.** Separately from this
  design's own state-partition tracking, swupdate tries to persist an
  "installed" marker via `/etc/fw_env.config`, which doesn't exist here,
  and reports the whole update as *failed* even after a genuinely
  successful write. Fixed with swupdate's own sanctioned
  `-m`/`--no-state-marker` flag -- this design tracks A/B state itself
  and doesn't use swupdate's mechanism for it.
- **No `ext4write` in this U-Boot build.** `qemu_arm64_defconfig`'s
  U-Boot has no ext4 write support at all (confirmed: `ext4write` is an
  unknown command, and the generic `save` command's ext4 backend also
  fails to write) -- but does have `fatwrite`. This is the mirror image
  of the real AM335x hardware's own U-Boot, which has `ext4write` but no
  FAT write command. `boot` and `state` are `vfat` here, not `ext4`, for
  exactly that reason -- pick whichever write path is actually real for
  the target, not whichever one happens to be more familiar.
