# Secure boot, verified live -- deep dive

Quick-start and the short version of this proof live in the top-level
[`README.md`](../README.md). This file is the full detail: exact
commands, exact console output, and the three real problems fixed to
get here.

## Booting through the real chain, not `runqemu`'s dev loop

`runqemu`'s default dev-loop boots the kernel directly (`-kernel
fitImage` straight to `qemu-system-aarch64`), which bypasses U-Boot
entirely. To exercise the real boot chain, U-Boot itself has to be
QEMU's bootloader, with the built rootfs attached as a virtio-blk disk:

```
qemu-system-aarch64 -machine virt -cpu cortex-a57 -smp 1 -m 512 -nographic \
    -bios build/tmp/deploy/images/qemuarm64/u-boot.bin \
    -kernel build/tmp/deploy/images/qemuarm64/fitImage \
    -drive file=build/tmp/deploy/images/qemuarm64/core-image-minimal-qemuarm64.rootfs.ext4,if=none,format=raw,id=hd0 \
    -device virtio-blk-device,drive=hd0 \
    -serial mon:stdio
```

At the `=>` prompt, set boot arguments, reload the kernel cleanly via
QEMU's fw_cfg device, and boot it, naming the FIT configuration
explicitly (`u-boot`'s `CONFIG_FIT_BEST_MATCH` otherwise looks for a
`compatible` match against the board's own devicetree, which a
bare kernel-only config doesn't set):

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

The `+` marks a signature-backed verification, not a bare hash check
-- this is `je-secureboot`'s RSA key (`je-secureboot-keys/je-secureboot-dev`)
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

## What made this non-trivial

Three real, distinct problems, each fixed in this repo rather than
worked around:

- **The verifying key wasn't embedded.** `je-secureboot.bbclass`
  deliberately doesn't (and can't) inherit `uboot-sign.bbclass` on the
  u-boot recipe or touch its `.config` -- that's the consuming BSP's
  job, done here in
  `sources/meta-je-example/recipes-bsp/u-boot/u-boot_%.bbappend`.
  `qemu_arm64_defconfig` also needed one non-obvious fix:
  `UBOOT_DTB_BINARY` has to be a flat filename copied into place after
  `do_compile` (`arch/arm/dts/qemu-arm64.dtb` on this board), not the
  nested path where the real dtb lives -- `uboot-sign.bbclass` uses
  the same variable both as a build-relative path and a bare deploy
  filename.
- **No devicetree was reaching the kernel at all.** Unlike a real
  board, qemuarm64's `virt` machine has no static in-tree devicetree
  source -- QEMU generates one dynamically per invocation. Without it,
  Linux couldn't even find a console and just hung after
  decompression. `sources/meta-je-example/recipes-bsp/devicetree/`
  captures that generated tree once and provides it through the
  standard `virtual/dtb` mechanism, so `kernel-fitimage.bbclass`'s
  real, unmodified pipeline embeds and signs it exactly like it would
  a real board's dts.
- **A hex-vs-decimal typo, and a phantom device.**
  `UBOOT_LOADADDRESS` needs an explicit `0x` prefix -- `dtc` reads a
  bare numeral in a FIT's `<...>` cell as decimal, so a naive
  `"48000000"` silently became decimal 48,000,000 instead of the
  intended address, and U-Boot hung decompressing the kernel into the
  wrong memory. Separately, a devicetree captured *without* `-bios`
  still describes a PL061 GPIO controller that QEMU only creates when
  no firmware/bootloader is present -- booting through U-Boot with
  that devicetree made the kernel probe a bus address with nothing
  behind it and panic. Both are fixed in `kas.yml` and the devicetree
  capture; see the recipe's comments for the exact failure signatures.

## What this does and doesn't prove

See `meta-justembed-security`'s
[`docs/update-boot.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/update-boot.md)
for the stage-by-stage trust-chain table -- QEMU has no boot-ROM
concept, so the verified chain here starts *at* U-Boot, not at an
immutable hardware root of trust. This is a real, cryptographically
sound FIT verification; it is not "full secure boot."
