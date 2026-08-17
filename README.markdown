 # Terrain

Terrain provides the primary REST API used by the Discovery Environment (DE). Its role is to validate user
authentication and to coordinate calls to other web services.

## pre-commit hooks

Install pre-commit hooks with `pre-commit install` for some basic checks.

## Local Development Testing

Terrain acts primarily as a front end to other Discovery Environment microservices, so the easiest way to test it by far
is to test it against a Kubernetes cluster running the most recent version of the Discovery Environment. You'll need
some prerequisites to get started.

### Prerequisites

- A recent version of Java. We normally use [OpenJDK][1].
- [Leiningen][2] - a dependency management and build system for Clojure.
- [jq][3] - a JSON parser (not required but extremely helpful).
- [kubectl][4] - used to manage Kuberenetes resources.
- [kubefwd][5] - used to forward local connections into a Kubernetes cluster.

### Preparation

These instructions use a few different environment variables as placeholders for values that may vary.

- `$NAMESPACE` refers to the Kubernetes namespace where the DE is installed. For example, you might use the `qa`
  namespace in a quality assurance environment and the `prod` namespace ina production environment.
- `$CONF` refers to teh path to the Terrain configuration file, which can be in any directory on your local
  computer. For example, it could be `/etc/cyverse/terrain.properties` or simply `terrain.properties` if the file is in
  your working directory.
- `$DE_USER` refers to the username that you would normally use to log into the Discovery Environment.

#### Obtain Kubernetes Cluster Access

This step is likely to be different for every deployment. Check with your administrator to obtain access.

#### Prepare a Configuration File

If you're using an existing DE deployment then this step is fairly easy; you can simply obtain a copy of the
configuration file from the Kubernetes cluster:

```
$ kubectl -n $NAMESPACE get secret service-configs -o json | jq -r '.data["terrain.properties"] | @base64d' \
    > "$CONF"
```

You should be able to use this file without modification.

#### Forward Connections to Kubernetes

This step requires [kubefwd][5], which automatically configures port forwarding and host name aliases. In another
terminal window or tab, run these commands:

```
$ sudo su -
# export KUBECONFIG=/path/to/kubernetes/config.conf
# kubefwd svc -n $NAMESPACE
```

You'll have to leave this terminal window or tab open while you're running Terrain locally.

#### Start Terrain

You'll need Leiningen and Java for this step, and this command should be executed from within your clone of the Terrain
repository.

```
$ lein run -- -c "$CONF"
```

After the service starts, the first step is to obtain an access token so that you can call other endpoints. For example:

```
$ export AUTH_HEADER="Authorization: bearer $(curl -su "$DE_USER" "http://localhost:60000/terrain/token" | jq -r .access_token)"
```

You can run this command to verify that the command worked:

```
$ echo $AUTH_HEADER
```

If the value is `Authorization: berer null` then the authentication didn't work. In most cases, it means that the
password was mistyped. Once you have the authorization header, you can make calls to other endpoints:

```
$ curl -sH "$AUTH_HEADER" "http://localhost:60000/terrain/apps?search=Word"
```

[1]: https://openjdk.org/
[2]: https://leiningen.org/
[3]: https://jqlang.org/
[4]: https://kubernetes.io/docs/reference/kubectl/
[5]: https://github.com/txn2/kubefwd
