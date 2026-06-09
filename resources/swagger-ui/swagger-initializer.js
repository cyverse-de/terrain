// Custom Swagger UI initializer for terrain.
//
// This file shadows the copy shipped in the ring-swagger-ui jar (terrain's resources directory precedes
// dependency jars on the classpath). In addition to the stock behavior of loading the API spec, it wires up the
// OAuth2 "Authorize" button for the OIDC Authorization Code Flow by calling initOAuth with the Keycloak client
// id and scopes supplied (config-driven) via ./config.json.
window.onload = function () {
  fetch("./config.json")
    .then(function (response) { return response.json(); })
    .then(function (config) {
      var redirectUrl =
        window.location.origin +
        window.location.pathname.replace(/\/$/, "") +
        "/oauth2-redirect.html";

      window.ui = SwaggerUIBundle({
        url: config.url,
        dom_id: "#swagger-ui",
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout",
        oauth2RedirectUrl: redirectUrl
      });

      window.ui.initOAuth({
        clientId: config.oauth2ClientId,
        scopes: config.oauth2Scopes,
        usePkceWithAuthorizationCodeGrant: true
      });
    });
};
