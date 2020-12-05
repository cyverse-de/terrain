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

Names of query paramters should be all lower case, with words separated by hyphens. This is a common query parameter
naming convention in REST APIs.

### Timestamp Formats

All new timestamps in both response and requests bodies should either be in [ISO 8601][1] format or a variant of ISO
8601 that supports up to nanosecond precision. The goal is to make timestamps in both request and response bodies human
readable.

### List Sorting

Endpoints in Terrain typically use the query parameters, `sort-field` and `sort-dir` to specify the field to use for
sorting and the direction to sort the results in (either ascending or descending). The values in `sort-field` will vary
depending on the specific endpoint, but they typically should correspond to values that are present in the response body
so that someone using the API can easily guess appropriate values to use. The values in `sort-dir` should be `ASC`,
`DESC`, `asc`, or `desc`. Case insensitivity is not required, but accepting either all-uppper-case or all-lower-case
versions of these values will help make the API more user friendly.

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
[go-swagger][6] can be used in two modes. One mode generates source code for the microservice from the [OpenAPI][2]
definition. This can provide a quick way to get started on an API, but in our experience, maintaining microservices
generated in this manner can be cumbersome. We've found that the better option is to generate the [OpenAPI][2]
definition from the source code. You can find documentation for this approach in the [go-swagger documentation][9]. A few
Go microservices used by Terrain, including [notifications][7] and [de-stats][8] use this approach successfully. See the
`Makefile` in each repository for examples of the commands used to generate the API specifications. One important caveat
is that when you're using this approach, the repository must be checked out into your GOPATH. For example, if you wish
to do work on the notifications service, the repository should be checked out into
`$GOPATH/src/github.com/cyverse-de/notifications`.

[1]: https://en.wikipedia.org/wiki/ISO_8601
[2]: https://swagger.io/resources/open-api/
[3]: https://github.com/metosin/compojure-api
[4]: https://github.com/plumatic/schema
[5]: https://github.com/cyverse-de/common-swagger-api/
[6]: https://goswagger.io/
[7]: https://github.com/cyverse-de/notifications
[8]: https://github.com/cyverse-de/de-stats
[9]: https://goswagger.io/use/spec.html
