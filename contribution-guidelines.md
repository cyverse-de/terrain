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
