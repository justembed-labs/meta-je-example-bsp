SUMMARY = "Constant bootstate=trying/good marker files for the boot partition"
DESCRIPTION = "\
U-Boot's trial-boot revert logic needs to write a fixed, known-good byte \
blob to the state partition via ext4write, which needs the bytes already \
in RAM first -- these two tiny files are that source blob, deployed \
alongside the built image so the disk-assembly step (wic) can place them \
on the boot partition. Deploy-only, not installed into any rootfs."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://bootstate-trying.env \
    file://bootstate-good.env \
"

inherit deploy

do_deploy() {
    install -d ${DEPLOYDIR}
    install -m 0644 ${WORKDIR}/bootstate-trying.env ${DEPLOYDIR}/bootstate-trying.env
    install -m 0644 ${WORKDIR}/bootstate-good.env ${DEPLOYDIR}/bootstate-good.env
}
addtask deploy before do_build after do_compile
