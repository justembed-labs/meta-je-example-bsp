SUMMARY = "swupdate.sh conf.d drop-in: webserver mode + mandatory verification key"
DESCRIPTION = "\
Also installs /etc/hwrevision -- swupdate refuses to install anything \
without it (or an explicit -H on the command line): confirmed real, \
'HW compatibility not found' with this file absent, even with a \
correctly signed .swu. Space-separated '<boardname> <revision>' -- \
confirmed against swupdate's own core/hw-compatibility.c: \
get_hw_revision() reads it with fscanf(fp, \"%ms %ms\", ...), NOT \
colon-separated like the -H flag's own <board>:<rev> syntax. \
\
swupdate-dev.htdigest is a checked-in throwaway credential for the \
webserver's --global-auth-file (HTTP Digest, real format confirmed \
against swupdate's own mongoose_interface.c: USER:DOMAIN:MD5(user:domain:password)) \
-- same 'dev, not production' precedent as swupdate-dev-priv.pem."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://je-swupdate-conf.sh file://swupdate-dev.htdigest"

RDEPENDS:${PN} = "swupdate je-downgrade-guard"

do_install() {
    install -d ${D}${libdir}/swupdate/conf.d
    install -m 0644 ${WORKDIR}/je-swupdate-conf.sh ${D}${libdir}/swupdate/conf.d/50-je-swupdate-conf.sh
    sed -i -e 's|@LIBDIR@|${libdir}|g' ${D}${libdir}/swupdate/conf.d/50-je-swupdate-conf.sh
    install -d ${D}${sysconfdir}
    echo "${MACHINE} 1.0" > ${D}${sysconfdir}/hwrevision
    install -m 0600 ${WORKDIR}/swupdate-dev.htdigest ${D}${sysconfdir}/swupdate-web.htdigest
}

FILES:${PN} = "${libdir}/swupdate/conf.d/50-je-swupdate-conf.sh ${sysconfdir}/hwrevision ${sysconfdir}/swupdate-web.htdigest"
