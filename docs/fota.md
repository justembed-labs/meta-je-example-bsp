# FOTA, verified live -- deep dive

Quick-start and the short version of this proof live in the top-level
[`README.md`](../README.md). This file is the full detail: exact
commands, exact console output, and the four real problems fixed to
get here.

`je-swupdate-fota` is wired into this build with a real dual-copy
(A/B) partition layout, a real signed `.swu` bundle, and a full update
cycle proven end to end on QEMU -- including the negative case, which
is the part that actually matters for an atomic-update design.

The disk (`core-image-minimal-qemuarm64.rootfs.wic`, built by adding
`wic` to `IMAGE_FSTYPES`) carries four partitions: a small `boot`
partition holding two constant marker files, two full rootfs copies
(`rootA`, `rootB`), and a small `state` partition for the active
partition number and trial-boot flag. `bitbake -c swuimage
update-image` builds a signed `.swu` containing the new rootfs plus
two activation scripts (one per target copy).

## Applying an update for real

```
/sbin/je-swupdate-select /path/to/update.swu
```

reads the running system's own `root=` from `/proc/cmdline` to pick
the *other* copy, then runs `swupdate -i ... -e stable,copyN -k
/etc/swupdate.pem -m` -- real RSA signature verification, a real raw
write to the inactive partition, then a real shellscript writing
`rootpart=N` / `bootstate=trial` to the state partition:

```
[INFO ] : SWUPDATE running :  Installation in progress
[INFO ] : SWUPDATE successful ! SWUPDATE successful !
```

On the next boot, U-Boot reads that state, sees `bootstate=trial`, and
arms a revert by writing a `bootstate=trying` marker before booting
the new copy -- exactly the trial-boot pattern a real board's own
compiled-in boot script would run, driven here by hand at the U-Boot
prompt since QEMU's generic `virt` machine has no such script to begin
with:

```
=> printenv rootpart bootstate
rootpart=3
bootstate=trial
=> load virtio 0:1 44000000 bootstate-trying.env
=> fatwrite virtio 0:4 44000000 bootstate $filesize
=> setenv bootstate trying
```

**If the system boots and confirms itself healthy**
(`je-swupdate-commit` running once multi-user boot is reached), the
next U-Boot check reads `bootstate=good` and boots the new copy again,
unconditionally -- confirmed by mounting the exact same, still-new
rootfs partition a boot later.

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

## What made this non-trivial

- **`je-swupdate-select` hardcoded `/dev/vda`.** QEMU doesn't
  guarantee that letter -- attaching a second virtio-blk disk (even
  just to carry the `.swu` file into the guest for testing) can
  enumerate this disk as `vdb` instead. Fixed to match any `vd[a-z]`
  letter, the same reasoning the real hardware version already
  applies across `mmcblkN` controllers.
- **Missing `/etc/hwrevision`.** swupdate refuses to install anything
  without it (`HW compatibility not found`) -- `je-swupdate-conf`
  installs it.
- **swupdate's own bootloader-env bookkeeping.** Separately from this
  design's own state-partition tracking, swupdate tries to persist an
  "installed" marker via `/etc/fw_env.config`, which doesn't exist
  here, and reports the whole update as *failed* even after a
  genuinely successful write. Fixed with swupdate's own sanctioned
  `-m`/`--no-state-marker` flag -- this design tracks A/B state
  itself and doesn't use swupdate's mechanism for it.
- **No `ext4write` in this U-Boot build.** `qemu_arm64_defconfig`'s
  U-Boot has no ext4 write support at all (confirmed: `ext4write` is
  an unknown command, and the generic `save` command's ext4 backend
  also fails to write) -- but does have `fatwrite`. This is the
  mirror image of the real AM335x hardware's own U-Boot, which has
  `ext4write` but no FAT write command. `boot` and `state` are `vfat`
  here, not `ext4`, for exactly that reason -- pick whichever write
  path is actually real for the target, not whichever one happens to
  be more familiar.

See `meta-justembed-security`'s
[`docs/update-boot.md`](https://github.com/justembed-labs/meta-justembed-security/blob/main/docs/update-boot.md)
for the firmware-update flow diagram and its honest limitation (a
binary/shallow health decision -- "reached multi-user boot," not an
application-level check).
