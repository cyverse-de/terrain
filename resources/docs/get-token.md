This service allows users to obtain OAuth tokens for accessing other API endpoints. You must be logged in using HTTP
basic authorization to use this endpoint. This is the only endpoint that uses basic authorization. To log in, click the
Authorize button above, enter your username and password under `Basic authentication`, and click the Authorize button
underneath the password text box.

Once you have the access token, you can use it to authorize calls to other endpoints in the Swagger UI.  First, remove
the basic authentication credentials by clicking the Authorize button above and clicking the Logout button in the `Basic
authenitcation` section of authorization window. Second, click the Authorization button again and type the word `Bearer`
followed by a single space in the Value text box of the `Api key authorization` section of the window. Paste in the
access token from this endpoint's response body then click the Authorize button underneath the Value text box.

You can use `curl` and `jq`, which is available from [the jq web site](https://stedolan.github.io/jq/), to obtain an
access token from the command line. The easiest way to do this on Unix-like operating systems is to define an
environment variable containing the authorization header:

```
export AUTH_HEADER=\"Authorization: Bearer $(curl -su username https://de.cyverse.org/terrain/token \
    | jq -r .access_token)\"
```

Once you have the authorization header stored in an environment variable, you can include it in calls to other Terrain
endpoints:

```
curl -sH \"$AUTH_HEADER\" \"https://de.cyverse.org/terrain/apps?search=word\"
```
