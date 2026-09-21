SUMMARY = "Constant bootstate=trying/good marker files for the boot partition"
DESCRIPTION = "\
U-Boot's trial-boot revert logic needs to write a fixed, known-good byte \
blob to the state partition via ext4write, which needs the bytes already \
in RAM first -- these two tiny files are that source blob, deployed \
alongside the built image so the disk-assembly step (wic) can place them \
on the boot partition. Deploy-only, not installed into any rootfs."

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

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
