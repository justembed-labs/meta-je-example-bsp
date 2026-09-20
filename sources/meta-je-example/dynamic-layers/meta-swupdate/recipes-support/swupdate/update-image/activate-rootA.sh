#!/bin/sh
# Runs on-target as a swupdate "copy1" script -- writes rootpart=2 to
# the state partition (/dev/vda4), the override u-boot's bootstate_check
# reads before computing which rootfs partition to boot.
#
# MUST only act on $1 = postinst: swupdate's "shellscript" handler runs
# this same script at preinst, postinst AND postfailure, passing the
# phase as $1 -- listing images before scripts in sw-description does
# NOT control execution order. Without this check it would run (and
# could arm trial-boot) at preinst, before the raw image write even
# started, and at postfailure, after a write that failed -- exactly the
# two cases activation must never happen.
[ "$1" = "postinst" ] || exit 0
#
# Also arms the trial-boot revert: bootstate=trial in the state file
# tells u-boot's bootstate_check this is an unconfirmed activation -- it
# rewrites its own bootstate file to "trying" before booting, and
# reverts rootpart automatically if a later boot ever sees "trying"
# again (never got confirmed). je-swupdate-commit clears this after a
# real successful boot.
set -e
mp=$(mktemp -d)
mount /dev/vda4 "$mp"
printf 'rootpart=2\nbootstate=trial\n' > "$mp/uEnv.txt"
sync
umount "$mp"
rmdir "$mp"
