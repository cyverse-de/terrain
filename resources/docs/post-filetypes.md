Sets or clears a file type. The file type can be set by passing in the name of one of the supported file types, which
can be obtained by calling the `GET /secured/filetypes/type-list` endpoint, for example:

``` json
{
    "path": "/path/to/irods/file",
    "type": "csv"
}
```

The file type can be cleared by passing in the empty string as the file type:

``` json
{
    "path": "/path/to/irods/file",
    "type": ""
}
```
