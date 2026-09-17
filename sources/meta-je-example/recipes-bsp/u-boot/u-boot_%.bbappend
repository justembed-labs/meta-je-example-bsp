# je-secureboot only signs the kernel/DT FIT (kernel-fitimage.bbclass);
# embedding the verifying public key into u-boot.dtb is uboot-sign.bbclass,
# which must be inherited directly on the u-boot recipe. Same
# UBOOT_SIGN_KEYDIR/KEYNAME as je-secureboot.bbclass (set globally via
# INHERIT, already in this recipe's datastore).
inherit uboot-sign

# uboot-sign.bbclass uses UBOOT_DTB_BINARY both as a path relative to ${B}
# (concat_dtb's -e check) and as a bare deploy filename (the final
# ln -sf ${DEPLOYDIR}/${UBOOT_DTB_BINARY} in do_deploy) -- it must stay a
# flat name, not a subdirectory path. qemu_arm64_defconfig's real dtb
# output is arch/arm/dts/qemu-arm64.dtb (CONFIG_DEFAULT_DEVICE_TREE), with
# no top-level u-boot.dtb copy, so copy it into place after compile
# (confirmed via two real build failures: first a silent skip with
# UBOOT_DTB_BINARY left at the class default, then a do_deploy ln failure
# after pointing UBOOT_DTB_BINARY at the nested path directly).
do_compile:append() {
	cp ${B}/arch/${UBOOT_ARCH_DIR}/dts/qemu-arm64.dtb ${B}/u-boot.dtb
}
