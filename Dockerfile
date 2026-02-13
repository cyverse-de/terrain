# Build stage
FROM clojure:temurin-25-lein-trixie AS builder

WORKDIR /usr/src/app

# Copy project file first to cache dependencies separately from source code
COPY project.clj /usr/src/app/
RUN lein deps

# Now copy source code and build
COPY . /usr/src/app
RUN lein do clean, uberjar

# Runtime stage - use minimal JRE image
FROM eclipse-temurin:25-jre-noble

WORKDIR /usr/src/app

# Create required crypto directories
RUN mkdir -p /etc/iplant/de/crypto && \
    touch /etc/iplant/de/crypto/pubring.gpg && \
    touch /etc/iplant/de/crypto/random_seed && \
    touch /etc/iplant/de/crypto/secring.gpg && \
    touch /etc/iplant/de/crypto/trustdb.gpg

# Copy JAR and config from builder
COPY --from=builder /usr/src/app/target/terrain-standalone.jar /usr/src/app/terrain-standalone.jar
COPY conf/main/logback.xml /usr/src/app/logback.xml

# Add the Internet2 InCommon intermediate CA certificate
ADD "https://uit.stanford.edu/sites/default/files/2023/10/11/incommon-rsa-ca2.pem" "/usr/local/share/ca-certificates/"
RUN sed -i -E 's/\r\n?/\n/g' "/usr/local/share/ca-certificates/incommon-rsa-ca2.pem" && \
    update-ca-certificates

# Create symlink for terrain command
RUN ln -s /opt/java/openjdk/bin/java /bin/terrain

CMD ["--help"]

ENTRYPOINT ["terrain", "-Dlogback.configurationFile=/usr/src/app/logback.xml", "-cp", ".:terrain-standalone.jar", "terrain.core"]
