# Real board BSPs ship a static in-tree devicetree source that
# kernel-fitimage.bbclass embeds directly. qemuarm64's "virt" machine has
# none -- QEMU generates its devicetree dynamically per invocation, so
# there's nothing for the kernel source tree to ship. This recipe
# captures that generated tree once and provides it through the same
# virtual/dtb mechanism a real vendor-supplied dts would use, so
# kernel-fitimage.bbclass's real, unmodified signing pipeline embeds and
# signs it like any other board.
#
# Captured via qemu-system-aarch64 -machine virt,dumpdtb=... using the
# EXACT full real boot command line (-bios/-kernel/-drive/-device
# included, not just -machine/-cpu/-smp/-m) -- QEMU's virt machine
# conditionally omits the PL061 GPIO controller (gpio-keys poweroff)
# when -bios/firmware is present. A devicetree captured without -bios
# still describes that pl061 device, which doesn't actually exist in
# this boot mode: the kernel's AMBA bus probe (amba_read_periphid) then
# reads a real bus address with nothing behind it and panics with a
# synchronous external abort. Confirmed via two real boot failures with
# byte-identical panic signatures, both fixed only once the capture
# invocation matched the real one exactly.
SUMMARY = "QEMU virt machine devicetree, captured for je-secureboot's FIT signing"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

INHIBIT_DEFAULT_DEPS = "1"
PROVIDES = "virtual/dtb"
PACKAGE_ARCH = "${MACHINE_ARCH}"
SYSROOT_DIRS += "/boot/devicetree"
FILES:${PN} = "/boot/devicetree/*.dtb"

COMPATIBLE_MACHINE = "qemuarm64"

SRC_URI = "file://qemu-virt.dtb"
S = "${WORKDIR}"

do_install() {
	install -Dm 0644 ${S}/qemu-virt.dtb ${D}/boot/devicetree/qemu-virt.dtb
}
