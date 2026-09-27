SUMMARY = "swupdate .swu bundle for this example's dual-copy layout"
DESCRIPTION = "\
Board-specific swupdate update-image recipe + sw-description -- the \
half that genuinely can't be generic (real partition device paths, \
this board's u-boot state-partition activation mechanism), per \
meta-je-boot-update's architecture decision. je-swupdate-fota \
(meta-justembed-security) provides the generic half: swupdate \
installed on-target, ext4.gz output. \
\
Two named copies (stable,copy1 / stable,copy2), each writing the raw \
rootfs image to its own partition (/dev/vda2 / /dev/vda3) and then \
running a shellscript that drops uEnv.txt on the state partition -- not \
swupdate's usual uboot: stanza, which needs a libubootenv-configured \
persistent env store this example doesn't have."

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit swupdate

# swupdate.bbclass's own IMAGE_DEPENDS default is empty -- without this,
# do_swuimage fails looking for a deploy artifact nothing ever built.
IMAGE_DEPENDS = "core-image-minimal"

SRC_URI = " \
    file://sw-description \
    file://activate-rootA.sh \
    file://activate-rootB.sh \
"

# meta-swupdate's search is a literal concatenation:
# <image><IMAGE_MACHINE_SUFFIX><this-fstype-value> -- modern Yocto's
# actual deploy filename inserts IMAGE_NAME_SUFFIX (".rootfs") before
# the fstype extension, which meta-swupdate's own naming convention
# predates, so it has to be spelled out here rather than just ".ext4.gz".
SWUPDATE_IMAGES = "core-image-minimal"
SWUPDATE_IMAGES_FSTYPES[core-image-minimal] = ".rootfs.ext4.gz"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[core-image-minimal] = "0"

# Dev keypair -- see files/README.md. Matches the target-side pubkey
# baked into je-swupdate-select.
SWUPDATE_SIGNING = "RSA"
SWUPDATE_PRIVATE_KEY = "${THISDIR}/files/swupdate-dev-priv.pem"
