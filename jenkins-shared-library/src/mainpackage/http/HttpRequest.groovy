package mainpackage.http

import mainpackage.LogLevel

public class HttpRequest {

    private String url
    private String auth
    private def headers
    private String desiredResponseCode
    private Script steps

    public HttpRequest(Map args) {

        if (!args.url || !args.auth || !args.steps) {
            throw new IllegalArgumentException("При создании экземпляра HttpRequest необходимо передать параметры url, auth и steps")
        }

        this.url = args.url
        this.auth = args.auth
        this.steps = args.steps
        this.headers = args.headers ?: []
        this.desiredResponseCode = args.desiredResponseCode ?: "100:404"
    }

    public def get() {
            return steps.httpRequest(
                    customHeaders: this.headers,
                    url: this.url,
                    httpMode: "GET",
                    authentication: this.auth,
                    ignoreSslErrors: "true",
                    validResponseCodes: this.desiredResponseCode,
                    quiet: true
            )
    }

    public def post(def body) {
        steps.withRetry() {
            return steps.httpRequest(
                    customHeaders: this.headers,
                    url: this.url,
                    httpMode: "POST",
                    authentication: this.auth,
                    ignoreSslErrors: "true",
                    validResponseCodes: this.desiredResponseCode,
                    quiet: true,
                    requestBody: body
            )
        }
    }
}
