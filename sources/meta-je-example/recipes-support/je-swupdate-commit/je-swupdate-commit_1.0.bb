SUMMARY = "Confirms a boot as good, disarming swupdate's trial-boot revert"
DESCRIPTION = "\
Writes bootstate=good to the state partition once normal multi-user \
boot is reached -- the userspace half of the trial-boot design. \
Without this, every activated copy would revert on its very next \
reboot regardless of whether it was actually healthy, since bootstate \
only ever starts at 'trial'/'trying' and nothing else clears it."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = " \
    file://je-swupdate-commit.sh \
    file://je-swupdate-commit.service \
"

inherit systemd

SYSTEMD_SERVICE:${PN} = "je-swupdate-commit.service"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/je-swupdate-commit.sh ${D}${sbindir}/je-swupdate-commit.sh

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/je-swupdate-commit.service ${D}${systemd_system_unitdir}/je-swupdate-commit.service
}

FILES:${PN} = " \
    ${sbindir}/je-swupdate-commit.sh \
    ${systemd_system_unitdir}/je-swupdate-commit.service \
"
