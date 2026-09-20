SUMMARY = "Picks the inactive A/B copy and invokes swupdate against it"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# swupdate has no compile-time public-key mechanism -- verification is
# runtime-only via a mandatory -k argument. swupdate-dev-pub.pem is the
# same dev keypair the .swu is signed with (see the update-image
# recipe's own README) -- a deliberate duplicate copy rather than a
# cross-directory FILESEXTRAPATHS reference (dynamic-layers/ paths
# aren't resolvable via ${LAYERDIR} from an unrelated recipe).
SRC_URI = "file://je-swupdate-select file://swupdate-dev-pub.pem"

RDEPENDS:${PN} = "swupdate"

do_install() {
    install -d ${D}${sbindir} ${D}${sysconfdir}
    install -m 0755 ${WORKDIR}/je-swupdate-select ${D}${sbindir}/je-swupdate-select
    install -m 0644 ${WORKDIR}/swupdate-dev-pub.pem ${D}${sysconfdir}/swupdate.pem
}

FILES:${PN} = "${sbindir}/je-swupdate-select ${sysconfdir}/swupdate.pem"
