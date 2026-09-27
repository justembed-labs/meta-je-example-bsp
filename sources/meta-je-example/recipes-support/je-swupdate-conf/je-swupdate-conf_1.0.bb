SUMMARY = "swupdate.sh conf.d drop-in: webserver mode + mandatory verification key"
DESCRIPTION = "\
Also installs /etc/hwrevision -- swupdate refuses to install anything \
without it (or an explicit -H on the command line): confirmed real, \
'HW compatibility not found' with this file absent, even with a \
correctly signed .swu. Space-separated '<boardname> <revision>' -- \
confirmed against swupdate's own core/hw-compatibility.c: \
get_hw_revision() reads it with fscanf(fp, \"%ms %ms\", ...), NOT \
colon-separated like the -H flag's own <board>:<rev> syntax."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://je-swupdate-conf.sh"

RDEPENDS:${PN} = "swupdate"

do_install() {
    install -d ${D}${libdir}/swupdate/conf.d
    install -m 0644 ${WORKDIR}/je-swupdate-conf.sh ${D}${libdir}/swupdate/conf.d/50-je-swupdate-conf.sh
    install -d ${D}${sysconfdir}
    echo "${MACHINE} 1.0" > ${D}${sysconfdir}/hwrevision
}

FILES:${PN} = "${libdir}/swupdate/conf.d/50-je-swupdate-conf.sh ${sysconfdir}/hwrevision"
