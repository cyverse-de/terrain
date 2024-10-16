FROM clojure:temurin-22-lein-jammy

WORKDIR /usr/src/app

RUN apt-get update && \
    apt-get install -y git && \
    rm -rf /var/lib/apt/lists/*

CMD ["--help"]

RUN mkdir -p /etc/iplant/de/crypto && \
    touch /etc/iplant/de/crypto/pubring.gpg && \
    touch /etc/iplant/de/crypto/random_seed && \
    touch /etc/iplant/de/crypto/secring.gpg && \
    touch /etc/iplant/de/crypto/trustdb.gpg

COPY conf/main/logback.xml /usr/src/app/

RUN ln -s "/opt/java/openjdk/bin/java" "/bin/terrain"

COPY . /usr/src/app
RUN lein do clean, uberjar && \
    mv target/terrain-standalone.jar . && \
    lein clean && \
    rm -r ~/.m2/repository

# Add the Internet2 InCommon intermediate CA certificate.
ADD "https://uit.stanford.edu/sites/default/files/2023/10/11/incommon-rsa-ca2.pem" "/usr/local/share/ca-certificates/"
RUN sed -i -E 's/\r\n?/\n/g' "/usr/local/share/ca-certificates/incommon-rsa-ca2.pem" && \
    update-ca-certificates

ENTRYPOINT ["terrain", "-Dlogback.configurationFile=/usr/src/app/logback.xml", "-cp", ".:terrain-standalone.jar", "terrain.core"]

ARG git_commit=unknown
ARG version=unknown
ARG descriptive_version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
LABEL org.cyverse.descriptive-version="$descriptive_version"
LABEL org.opencontainers.image.authors="CyVerse Core Software Team <support@cyverse.org>"
LABEL org.opencontainers.image.revision="$git_commit"
LABEL org.opencontainers.image.source="https://github.com/cyverse-de/terrain"
LABEL org.opencontainers.image.version="$descriptive_version"
