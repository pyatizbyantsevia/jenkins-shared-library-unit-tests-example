package mainpackage.http

import mainpackage.PipelineContext

/**
 * Wrapper for the Jenkins HTTP Request step.
 * <br>
 * Provides methods for sending GET and POST requests and supports retries in case of server errors.
 *
 * @see <a href="https://www.jenkins.io/doc/pipeline/steps/http_request/">HTTP Request Plugin Documentation</a>
 */
class HttpRequest {

    private String url
    private String auth
    private def headers
    private String desiredResponseCode
    private Script steps = PipelineContext.instance.getSteps()

    HttpRequest(Map args) {
        if (!args.url || !args.auth) {
            throw new IllegalArgumentException("When creating HttpRequest object, you must pass the url and auth parameters")
        }
        this.url = args.url
        this.auth = args.auth
        this.headers = args.headers ?: []
        this.desiredResponseCode = args.desiredResponseCode ?: "100:499"
    }

    def get() {
        steps.withRetry() {
            steps.httpRequest(
                    customHeaders: headers,
                    url: url,
                    httpMode: "GET",
                    authentication: auth,
                    ignoreSslErrors: "true",
                    validResponseCodes: desiredResponseCode,
                    quiet: true
            )
        }
    }

    /**
     * Performs a POST request with a raw body.
     *
     * @param body
     * @return The response to the request
     */
    def post(String body) {
        steps.withRetry() {
            steps.httpRequest(
                    customHeaders: headers,
                    url: url,
                    httpMode: "POST",
                    authentication: auth,
                    ignoreSslErrors: "true",
                    validResponseCodes: desiredResponseCode,
                    quiet: true,
                    requestBody: body
            )
        }
    }

    /**
     * Performs a POST request with form data.
     *
     * @param formData
     * @return The response to the request
     */
    def post(List formData) {
        steps.withRetry() {
            steps.httpRequest(
                    customHeaders: headers,
                    url: url,
                    httpMode: "POST",
                    authentication: auth,
                    ignoreSslErrors: "true",
                    validResponseCodes: desiredResponseCode,
                    quiet: true,
                    formData: formData
            )
        }
    }
}
