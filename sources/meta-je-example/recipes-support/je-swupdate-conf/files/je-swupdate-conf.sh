# Sourced by meta-swupdate's own swupdate.sh wrapper (SWUPDATE_ARGS/
# SWUPDATE_WEBSERVER_ARGS, the documented override mechanism).
. @LIBDIR@/swupdate/je-downgrade-guard.sh
SWUPDATE_ARGS="-v -k /etc/swupdate.pem $(je_downgrade_guard_args)"
# HTTP Digest auth (--global-auth-file, htdigest format), not TLS --
# no self-signed cert. The credential is a checked-in throwaway, same
# precedent as swupdate-dev-priv.pem; a real deployment provisions its
# own (`htdigest -c /etc/swupdate-web.htdigest swupdate-demo <user>`).
SWUPDATE_WEBSERVER_ARGS="--document-root /www --port 8080 --auth-domain swupdate-demo --global-auth-file /etc/swupdate-web.htdigest"
