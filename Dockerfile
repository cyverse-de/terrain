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

# Pre-load the class metadata terrain needs at startup into an AOT cache, roughly halving startup
# time. Trained here rather than in the builder stage because the cache is only usable by the exact
# JVM build that wrote it, and the two stages' base images are updated independently.
RUN terrain -XX:AOTCacheOutput=/usr/src/app/terrain.aot \
      -Dlogback.configurationFile=/usr/src/app/logback.xml \
      -cp terrain-standalone.jar \
      clojure.main -e "(require 'terrain.core 'terrain.routes 'ring.adapter.jetty)"

CMD ["--help"]

# The classpath must stay exactly as it was when the AOT cache above was written, or the cache is
# rejected at startup. Adding the working directory back would break it two ways: the dumper refuses
# a non-empty directory on the classpath, and a runtime classpath that does not match the dumped one
# is discarded. Nothing here needs it — logback is configured by absolute path, and everything else
# lives in the jar. A missing or rejected cache only logs an error; terrain still starts.
ENTRYPOINT ["terrain", "-Dlogback.configurationFile=/usr/src/app/logback.xml", "-XX:AOTCache=/usr/src/app/terrain.aot", "-cp", "terrain-standalone.jar", "terrain.core"]
