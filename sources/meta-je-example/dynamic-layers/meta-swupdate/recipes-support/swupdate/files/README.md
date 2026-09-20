# Development signing key -- DO NOT USE IN PRODUCTION

`swupdate-dev-priv.pem` / `swupdate-dev-pub.pem` are a throwaway RSA
keypair for getting signed `.swu` updates working end to end in this
example. Committed on purpose so the build is reproducible for anyone
cloning this repo, exactly like this.

Before any real deployment: generate a real keypair, keep the private
key out of git entirely (CI secret variable or an HSM, not a file in
this repo), and only the public key/certificate belongs here.

Regenerate the dev pair:

    openssl genrsa -out swupdate-dev-priv.pem 2048
    openssl rsa -in swupdate-dev-priv.pem -pubout -out swupdate-dev-pub.pem
