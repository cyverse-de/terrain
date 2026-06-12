// Custom Swagger UI initializer for terrain.
//
// This file shadows the copy shipped in the ring-swagger-ui jar (terrain's resources directory precedes
// dependency jars on the classpath). In addition to the stock behavior of loading the API spec, it wires up
// authentication for the OIDC Authorization Code Flow that is handled entirely server-side by terrain.
//
// Rather than using Swagger UI's native OAuth2 authorize modal (which would prompt for and transmit the client
// id and secret in the browser), the prominent "Authorize" button is overridden to redirect the browser to
// terrain's /oidc/login endpoint. Terrain performs the code-for-token exchange server-side using the configured
// client id and secret, then stores the access token in an HttpOnly cookie. That cookie is sent automatically
// with "Try it out" requests, so the client id and secret never reach the browser.

// Plugin that replaces Swagger UI's top-level "Authorize" button so that clicking it starts terrain's
// server-side OIDC Authorization Code Flow instead of opening the native credential modal. The button only
// renders when the spec declares at least one security definition, which is why a (bearer) definition is kept
// in the spec.
function OidcAuthorizeButtonPlugin() {
  return {
    wrapComponents: {
      // The factory receives (Original, system); Original is unused but must stay so that system is positional.
      authorizeBtn: function (Original, system) {
        return function () {
          var React = system.React;
          return React.createElement(
            "button",
            {
              className: "btn authorize unlocked",
              onClick: function () {
                window.location.href = window.location.origin + "/terrain/oidc/login";
              }
            },
            React.createElement("span", null, "Authorize"),
            React.createElement(
              "svg",
              { width: 20, height: 20 },
              React.createElement("use", { href: "#unlocked", xlinkHref: "#unlocked" })
            )
          );
        };
      }
    }
  };
}

window.onload = function () {
  fetch("./config.json")
    .then(function (response) { return response.json(); })
    .then(function (config) {
      window.ui = SwaggerUIBundle({
        url: config.url,
        dom_id: "#swagger-ui",
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl,
          OidcAuthorizeButtonPlugin
        ],
        layout: "StandaloneLayout",
        // Ensure the de-access-token cookie set by terrain's OIDC callback is sent with "Try it out" requests.
        requestInterceptor: function (request) {
          request.credentials = "include";
          return request;
        }
      });

      addLogoutLink();
    });
};

// Adds a small "Logout" link that clears the OIDC token cookie via terrain's /oidc/logout endpoint. Login is
// handled by the overridden "Authorize" button, so only a logout affordance is needed here.
function addLogoutLink() {
  var container = document.createElement("div");
  container.id = "terrain-auth-links";
  container.style.cssText = "padding: 8px 20px; text-align: right; background: #1b1b1b;";

  var logout = document.createElement("a");
  logout.href = window.location.origin + "/terrain/oidc/logout";
  logout.textContent = "Logout";
  logout.style.cssText = "color: #fff; text-decoration: underline; font-family: sans-serif;";

  container.appendChild(logout);

  var swaggerEl = document.getElementById("swagger-ui");
  swaggerEl.parentNode.insertBefore(container, swaggerEl);
}
