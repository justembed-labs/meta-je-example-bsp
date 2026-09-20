#!/bin/sh
# Writes bootstate=good to the state partition (/dev/vda4) -- disarms
# u-boot's trial-boot revert by making the *next* boot's bootstate check
# see "good" instead of "trying". Runs once, triggered by reaching
# normal multi-user boot -- an honest v1 confirmation signal (init
# completed, no crash/emergency shell), not a deep application-level
# health check.
set -e
mp=$(mktemp -d)
mount /dev/vda4 "$mp"
printf 'bootstate=good\n' > "$mp/bootstate"
sync
umount "$mp"
rmdir "$mp"
