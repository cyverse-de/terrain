# Terrain startup time and memory footprint

Date: 2026-08-17

## Problem

Terrain pods are slow to become ready and hold large memory reservations
(1Gi request / 3Gi limit) relative to what the service actually uses.

## Measured baseline

Loading `terrain.routes` from `terrain-standalone.jar` under the production JVM
options (`jvm_opts_high` = `-XX:MaxRAMPercentage=70 -XX:+UseG1GC
-XX:+ExitOnOutOfMemoryError`, 3Gi limit):

| Metric | Value |
| --- | --- |
| Wall time | 2.40 s |
| Peak RSS | 413 MB |
| Live heap after load | 49 MB |
| Metaspace | 71 MB |
| Classes loaded | 16,329 |

Two hypotheses were tested and rejected:

- **Heap sizing is not a lever.** Dropping `MaxRAMPercentage` from 70 to 25 left
  RSS unchanged (455 MB vs 443 MB). The footprint is Metaspace, code cache, and
  the mapped 90 MB jar, not heap.
- **The JDK 25 AOT cache is a startup lever, not a memory one.** It cuts load
  time to 1.26 s and Metaspace to 4 MB, but adds a ~100 MB mapped archive, for a
  net RSS increase (413 MB -> 443 MB). Deferred to a later change.

## Scope of this change

Startup latency as observed by Kubernetes, and uberjar size. Container resource
limits are explicitly out of scope: terrain has only been measured at startup,
never under load, and the file-upload path may legitimately need the headroom.

## Part 1: probe retune

All three probes set `initialDelaySeconds: 60`. Because a `startupProbe` gates
the liveness and readiness probes, its delay alone sets the floor — a pod cannot
report ready in under 60 s regardless of how fast the JVM starts. Measured JVM
load is 2.4 s.

New values:

| Probe | initialDelay | period | timeout | failureThreshold |
| --- | --- | --- | --- | --- |
| liveness | 0 | 20 | 10 | (default 3) |
| startup | 0 | 2 | 5 | 150 |
| readiness | 0 | 10 | 5 | (default 3) |

`initialDelaySeconds: 0` on liveness and readiness is safe precisely because the
startup probe gates them; that gating is what made the old 60 s delays redundant
in the first place.

The startup budget goes from 660 s (60 + 30x20) to 300 s (150x2), still roughly
a 60x margin over measured startup.

The probe target `GET /` (`src/terrain/routes/misc.clj`) returns a static 200
with no downstream calls, so the faster cadence costs nothing. Note that this
means readiness reflects "Jetty is listening", not "dependencies are reachable".

## Part 2: dependency diet

| Dependency | Exclusion | Rationale |
| --- | --- | --- |
| `clojurewerkz/elastisch` | `org.elasticsearch/elasticsearch` | Only the `clojurewerkz.elastisch.native.*` namespaces reference `org.elasticsearch` classes. Terrain uses `elastisch.rest` and `elastisch.rest.document`, which are pure clj-http/cheshire. Removes Elasticsearch 2.4.6, Lucene 5.5.4, Guava 18, Netty 3.10, and snakeyaml 1.15. |
| `org.cyverse/async-tasks-client` | `cider/cider-nrepl` | async-tasks-client 0.0.5 declares this development tool as a compile-scope dependency, so it ships to production. Worth an upstream fix as well. |
| `org.cyverse/clj-jargon` | `junit` | Pulled in as a compile-scope dependency of `jargon-data-utils`. |

Verified before implementation: the full route tree (`terrain.routes`) and the
search namespaces (`terrain.persistence.search`,
`terrain.services.metadata.tags`) load with all 27 corresponding jars removed
from the classpath.

### Considered and rejected

Moving `org.clojure/tools.nrepl` to the `:dev` profile. It is required at the
top of `terrain.core`, the AOT-compiled main namespace, so the uberjar would
fail to build. At 41 KB it does not justify restructuring `terrain.core`.

## Files

| Repo | File | Change |
| --- | --- | --- |
| terrain | `project.clj` | exclusions |
| terrain | `k8s/terrain.yml` | probes (skaffold/local dev only) |
| deployments | `ansible/roles/services/terrain/templates/k8s/terrain.yml.j2` | probes (deployed) |

`ansible/roles/services/terrain/files/k8s/terrain.yml` looks like a second copy
to keep in sync, but it is rendered output and is gitignored
(`ansible/.gitignore`). The template is the only source to edit.

## Verification

1. `lein deps :tree` before and after, to confirm exactly what was removed.
2. `lein uberjar`; compare jar size and class counts.
3. Re-run the startup and RSS benchmark; confirm no regression.
4. `lein test`.
5. Functional test of tag indexing against a live Elasticsearch. `es_enabled`
   defaults to true in deployments, so this path is active. Loading without the
   jar is not the same as working against a real server, and this is the one
   risk in the change that static verification cannot cover.

## Measured outcome

| Metric | Before | After |
| --- | --- | --- |
| Uberjar size | 86.2 MB | 64.5 MB (-25%) |
| Jar entries | 62,215 | 47,188 |
| Resolved artifacts | 206 | 175 |
| Startup (prod JVM opts, 3-run avg) | 2.31 s | 2.26 s |
| Peak RSS | 400 MB | 411 MB |
| Metaspace | 71 MB | 70 MB |
| Classes loaded | 16,417 | 16,280 |

**The dependency diet did not improve startup time or runtime memory**, which
matches the prediction that these jars were shipped but never loaded. Its value
is image size, pull time, and dropping Elasticsearch 2.4.6 / Lucene 5.5.4 /
Guava 18 / Netty 3.10 / snakeyaml 1.15 from the artifact. JVM startup itself is
addressed by the AOT cache in the follow-up change, not here.

Two resolution changes fell out of removing Elasticsearch, which had been
winning version resolution for both:

- Guava 18.0 -> 16.0.1 (a downgrade). Its only remaining consumer is
  `jackson-coreutils 1.9`, which declares 16.0.1 itself, so this is the version
  that library was built against. Verified by generating the full swagger spec
  (400 paths, 547 definitions), which exercises ring-swagger -> scjsv ->
  json-schema-validator -> Guava.
- snakeyaml 1.15 -> 1.23 (an upgrade), now resolved via clj-yaml.

Implementation note: `cider/cider-nrepl` is declared at compile scope by eleven
different `org.cyverse` libraries, so a per-dependency exclusion was replaced
with a project-level `:exclusions` entry.

## Part 3: the JDK 25 AOT cache

Implemented in the `Dockerfile` as a training run plus an `-XX:AOTCache` flag on
the entrypoint.

The training run lives in the **runtime** stage, not the builder. An AOT cache is
only usable by the exact JVM build that wrote it. Both stages happen to ship
Temurin 25.0.3+9 today, but their base images are updated independently, so
training in the runtime stage guarantees parity by construction instead of by
coincidence.

### The classpath constraint

The entrypoint classpath was `.:terrain-standalone.jar`. That does not work, in
two separate ways:

1. The dumper refuses a non-empty directory on the classpath outright:
   `Error: non-empty directory '.'`.
2. Dumping with a jar-only classpath and then running with `.` prepended is
   rejected at startup: `The name of app classpath [1] does not match: expected
   'terrain-standalone.jar', got '.'`.

So `.` was removed from the entrypoint classpath and the training run uses the
same jar-only classpath. Nothing needed the working directory: logback is
configured through `-Dlogback.configurationFile` with an absolute path (verified
that it resolves with the directory off the classpath), and `/usr/src/app`
otherwise contains only the jar.

### Failure mode

A missing, stale, or mismatched cache logs an error and starts normally. That is
good for availability but means a misconfigured cache is **invisible**: the
service builds, boots, and serves traffic correctly while delivering none of the
benefit. Confirm `Opened AOT cache` under `-Xlog:aot` rather than inferring
success from a clean startup.

### Results

| Metric | Before | After |
| --- | --- | --- |
| Startup, in-container (3-run avg, incl. container start) | 2.12 s | 1.07 s (-50%) |
| Startup, local, prod JVM opts | 2.35 s | 1.37 s (-42%) |
| Metaspace | 70 MB | 4 MB |
| Peak RSS | 452 MB | 443 MB |
| Image size | ~391 MB | 495 MB (+104 MB) |

Memory is a wash, as predicted: the cache trades anonymous Metaspace for a
file-backed mapped archive. The image grows by more than Part 2 removed, so the
net effect on image size across this work is roughly +82 MB in exchange for
halved startup.

## Out of scope, noted for follow-up

- `project.clj` writes `:exclusion` (singular) for kameleon, which is a silent
  no-op, so that exclusion has never taken effect.
- `replicas: 2` with `maxSurge: 200%` and `requiredDuringScheduling` host
  anti-affinity requires 6 distinct nodes for a rollout. On a smaller cluster
  the surge pods stay Pending and stall the rollout, which may matter more to
  rollout latency than JVM startup does.
- The JDK 25 AOT cache (measured above) as a separate change.
