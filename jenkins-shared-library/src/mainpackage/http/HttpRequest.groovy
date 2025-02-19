package mainpackage.http

class HttpRequest {

    private String url
    private String auth
    private def headers
    private String desiredResponseCode
    private Script steps

    HttpRequest(Map args) {
        if (!args.url || !args.auth || !args.steps) {
            throw new IllegalArgumentException("When creating a JenkinsApi object, you must pass the url, auth and steps parameters")
        }

        this.url = args.url
        this.auth = args.auth
        this.steps = args.steps
        this.headers = args.headers ?: []
        this.desiredResponseCode = args.desiredResponseCode ?: "100:404"
    }

    def get() {
        steps.httpRequest(
                customHeaders: this.headers,
                url: this.url,
                httpMode: "GET",
                authentication: this.auth,
                ignoreSslErrors: "true",
                validResponseCodes: this.desiredResponseCode,
                quiet: true
        )
    }

    def post(def body) {
        steps.httpRequest(
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
