# Sourced by meta-swupdate's own swupdate.sh wrapper (SWUPDATE_ARGS/
# SWUPDATE_WEBSERVER_ARGS, the documented override mechanism).
. @LIBDIR@/swupdate/je-downgrade-guard.sh
SWUPDATE_ARGS="-v -k /etc/swupdate.pem $(je_downgrade_guard_args)"
SWUPDATE_WEBSERVER_ARGS="--document-root /www --port 8080"
