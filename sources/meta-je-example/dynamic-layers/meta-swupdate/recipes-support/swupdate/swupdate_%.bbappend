FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Enables signature-verification support at compile time; the actual
# public key is runtime-only (swupdate's mandatory -k argument), no
# compile-time key file involved -- see swupdate-signing.cfg.
SRC_URI += "file://swupdate-signing.cfg"
