# Terrain Contribution Guidelines

## Objectives

All future changes to Terrain and the microservices that it calls should be made with these goals in mind:

- **Consistency**: Whenever possible, API endpoints should use consistent data formats and naming conventions.
- **Documentation**: REST APIs should all expose automatically generated API documentation whenever possible.
- **Robustness**: REST API endpoints should be written to be as fault tolerant as reasonably possible.
- **Debugability**: REST API endpoints should return appropriate HTTP status codes and helpful response bodies when an
  error occurs.
- **Performance**: REST API endpoints should be as responsive as possible.
- **Minimalism**: The Terrain API should contain the smallest number of endpoints possible to adequately satisfy our
  needs.

## Consistency

Terrain has evolved slowly over the span of nearly a decade now, and some inconsistencies have crept into the API over
the years. One of the goals of this document is to counteract that entropy.

### JSON Field Names

Terrain has traditionally used `snake_case` for most JSON field names, but that convention prevents JavaScript clients
from being able to use dot notation to access field values. For this reason, new field names in request and response
bodies should use `camelCase` instead. Exceptions for objects that are sufficiently similar to existing objects may be
made on a case-by-case basis. When an overhaul to an existing endpoint occurs, however, we should weigh whether to
convert field names.

### Query Parameter Names

Names of query parameters should be all lower case, with words separated by hyphens. This is a common query parameter
naming convention in REST APIs.

### Timestamp Formats

All new timestamps in both response and requests bodies should either be in [ISO 8601][1] format or a variant of ISO
8601 that supports up to nanosecond precision. The goal is to make timestamps in both request and response bodies human
readable.

### List Sorting

Endpoints in Terrain typically use the query parameters, `sort-field` and `sort-dir` to specify the field to use for
sorting and the direction to sort the results in (either ascending or descending). The values in `sort-field` will vary
depending on the specific endpoint, but they typically should correspond to values that are present in the response body
so that someone using the API can easily guess appropriate values to use. The values in `sort-dir` should be `asc` or
`desc`.

## Documentation

Whenever possible, CyVerse REST APIs should have online documentation provided by the services themselves. The
recommended practice is to use [OpenAPI][2] to describe every REST API, whether or not that API is public facing. Even
if an API is not publicly facing, API documentation can provide an extremely useful resource for debugging and new
development.

For Clojure development, we recommend using [compojure-api][3] to generate and serve interactive API
documentation. Compojure-api uses [plumatic/schema][4] to define request and response body formats, and it automatically
validates both request and response bodies to verify that they adhere to their respective schemas. Because several of
our Clojure services share common schema definitions, a shared library called [common-swagger-api][5] has been
created. When a new endpoint is created, care should be taken when considering where to place the schema definitions. In
general, if the schema definition is used in multiple Clojure microservices then the schema definitions should be placed
in [common-swagger-api][5]. Otherwise, the schema definitions should be appeear in the repository for the microservice
itself. In Terrain, schema definitions are placed in namespaces underneath `terrain.routes.schemas`.

For Go development, we recommend using [go-swagger][6] to generate and serve interactive API documentation. Note that
[go-swagger][6] can be used in two modes. One mode generates source code for the microservice from the [OpenAPI][2] API
specification. This can provide a quick way to get started on an API, but in our experience, maintaining microservices
generated in this manner can be cumbersome. We've found that the better option is to generate the [OpenAPI][2]
definition from the source code. You can find documentation for this approach in the [go-swagger documentation][9]. A
few Go microservices used by Terrain, including [notifications][7] and [de-stats][8] use this approach successfully. See
the `Makefile` in each repository for examples of the commands used to generate the API specifications. One important
caveat is that when you're using this approach, the repository must be checked out into your GOPATH. For example, if you
wish to do work on the notifications service, the repository should be checked out into
`$GOPATH/src/github.com/cyverse-de/notifications`.

## Robustness

Robustness refers to the fault tolerance of an endpoint. In general, DE service endpoints should respond to errors as
cleanly as possible. Care and consideration should be given when determining what to to in the event of an error. For
example, what happens when the endpoint can't connect to the database or times out while reading a response from another
service? In many cases, the error can be reported to the caller, but that might not always be the case. It may also be
the case that we can report the error to the caller, but some cleanup has to be done beforehand.

Sometimes, the service itself cannot respond to an error condition. For example, if a host that a service is running on
encounters a network failure. In these cases, the service should be restarted automatically as soon as possible. For
cases where the host itself encounters an error, merely having the service running inside Kubernetes will help. For
other failure conditions, we have to provide a way for Kubernetes to detect when something is wrong with the
service. Having useful and reliable health and readiness checks for each service will help to ensure that services will
be restarted if something goes wrong.

## Debugability

Debugability means that callers should be able to identify and respond to errors. For REST interfaces, this means that
HTTP status codes should be correct and that error response bodies should contain enough information for callers to
identify and correct the problem. For example, if a resource isn't found, an HTTP endpoint should return status
code 404. If there's a problem with the request then the status code should be in the 400 series, and a response body
providing additional information about the error should be returned. For new endpoints, the response body should look
like this:

``` JavaScript
{
    "message": "error details here"
}
```

Additional fields may be present, but the `message` field should always be included. Note that this is the default error
response body format used by [labstack/echo][10].

## Performance

The Discovery Environemnt sometimes makes multiple service calls to perform a single task, so it's important that every
endpoint in Terrain is as responsive as reasonably possible. We say "reasonably possible" because small performance
improvements can sometimes come at too high of a cost in terms of development effort. As a general rule of thumb,
endpoints should return within 2 seconds whenever possible. When considering performance (or looking for ways to improve
the performance of an endoint), consider the following questions:

- Is the endpoint duplicating work? For example, is the same value calculated in multiple different places? Can the
  endpoint benefit from a caching strategy of some sort?
- Are individual calculations being performed when a bulk calculation might be faster? For example, are multiple
  database calls being made when a single call could be used more efficiently?
- Can some tasks be done in parallel? A classic example of this is when Terrain is orchestrating calls to other
  services, and the API calls are not dependent on each other.
- Is a service endpoint doing too much? Sometimes its better to split up an endpoint so that clients can adjust how the
  tasks are being performed. For example, maybe an endpoint is always returning some piece of information that's only
  used in a small number of cases.

## Minimalism

Many APIs provide multiple ways to perform tasks. This can be convenient for advanced API users because it provide
flexibility. Unforunately, having multiple ways to peform tasks also has drawbacks. First, it makes the software more
difficult to maintain because pieces of code that perform related tasks need to be updated in tandem. Second, it makes
the API less approachable to new users. Whenever a new endpoint is being added (or a new feature is being added to an
existing endpoint), care should be taken to ensure that redundancy is not introduced.

In rare cases, it may be beneficial to introduce redundancy. For example, two endpoints might provide the same data but
be optimized for specific use cases. When this does occur, care should be taken to document the reason for the
redundancy in the API documentation.

[1]: https://en.wikipedia.org/wiki/ISO_8601
[2]: https://swagger.io/resources/open-api/
[3]: https://github.com/metosin/compojure-api
[4]: https://github.com/plumatic/schema
[5]: https://github.com/cyverse-de/common-swagger-api/
[6]: https://goswagger.io/
[7]: https://github.com/cyverse-de/notifications
[8]: https://github.com/cyverse-de/de-stats
[9]: https://goswagger.io/use/spec.html
[10]: https://github.com/labstack/echo
