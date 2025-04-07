package mainpackage.api

import mainpackage.LogLevel
import mainpackage.PipelineContext
import mainpackage.exception.InputRequestNotFoundException
import mainpackage.polling.Poller
import mainpackage.Result
import mainpackage.exception.UnexpectedResponseCodeException
import mainpackage.http.HttpRequest

/**
 * Interacts with Jenkins through its REST API.
 *
 * Provides methods for managing Jenkins items.
 */
class JenkinsAPI extends API {

    private Script steps = PipelineContext.getInstance().getSteps()

    /**
     * @param args named parameters.
     *     <ul>
     *       <li>{@code url} - Jenkins server (must end with '/')</li>
     *       <li>{@code credential} - The authentication Jenkins credential, kind: username with password</li>
     *     </ul>
     *     If any of these parameters are missing, an {@link IllegalArgumentException} will be thrown
     */
    JenkinsAPI(Map args) {
        super(args)
    }

    /**
     * Creates a new Jenkins job.
     *
     * @param jobName in human-readable format
     */
    void createJob(String jobName) {
        this.createItem(jobName, this.steps.libraryResource('mainpackage/api/simple-job.xml'))
    }

    /**
     * Creates a new Jenkins folder.
     *
     * @param folderName in human-readable format
     */
    void createFolder(String folderName) {
        this.createItem(folderName, this.steps.libraryResource('mainpackage/api/simple-folder.xml'))
    }

    /**
     * Creates a new Jenkins item.
     *
     * @param itemName in human-readable format, example: rootFolder/folderToCreate; rootFolder/jobToCreate
     * @param xml it defines your item
     */
    void createItem(String itemName, String xml) {
        def headers = [
                [name: 'Content-Type', value: 'application/xml']
        ]

        String itemToCreate = itemName.tokenize('/').last()
        String path = this.itemNameToUrl(itemName.tokenize('/').dropRight(1).join('/'))

        def response = new HttpRequest(
                headers: headers,
                url: this.url + path + '/createItem?name=' + itemToCreate,
                auth: this.credential
        ).post(xml)

        if (response.status == 400) {
            this.steps.log("Jenkins Item already exist: " + itemName, LogLevel.NOTICE)
        } else if (response.status != 200) {
            this.steps.log("Failure when creating Jenkins Item: " + itemName + ", with status: " + response.status, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }
    }

    /**
     * Updates existing Jenkins item.
     *
     * @param itemName in human-readable format, example: rootFolder/folderToUpdate; rootFolder/jobToUpdate
     * @param xml it defines your update
     */
    void updateItem(String itemName, String xml) {
        def headers = [
                [name: 'Content-Type', value: 'application/xml']
        ]

        String path = this.itemNameToUrl(itemName)

        def response = new HttpRequest(
                headers: headers,
                url: this.url + path + "/config.xml",
                auth: this.credential
        ).post(xml)

        if (response.status != 200) {
            this.steps.log("Failure when updating Jenkins Item: " + itemName + ", with status: " + response.status, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }
    }

    /**
     * Retrieves the api/xml data for a specific build.
     *
     * @param itemName in human-readable format
     * @param buildNumber
     * @return the XML data for the specified build
     */
    def getBuildApiXml(String itemName, String buildNumber) {
        String path = this.itemNameToUrl(itemName)
        return this.getApiXml(this.url + path + "/${buildNumber}")
    }

    /**
     * Retrieves the api/xml data from a given URL.
     *
     * @param itemUrl the full URL of the item
     * @return the XML data retrieved from the URL
     */
    def getApiXml(String itemUrl) {
        def response = new HttpRequest(
                url: itemUrl + "/api/xml",
                auth: this.credential
        ).get()

        if (response.status != 200) {
            this.steps.log("Failed to get information: " + itemUrl, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }

        return response.content
    }

    /**
     * Press "Proceed" in input-step.
     *
     * @param buildUrl the full build URL
     * @param inputId custom input ID
     */
    void simpleProceed(String buildUrl, String inputId) {
        def headers = [
                [name: 'Content-Type', value: 'application/x-www-form-urlencoded']
        ]

        def body = "json=%7B%22parameter%22%3A%5B%5D%7D"

        def response = new HttpRequest(
                headers: headers,
                url: buildUrl + "/wfapi/inputSubmit?inputId=" + inputId,
                auth: this.credential
        ).post(body)

        if (response.status != 200) {
            steps.log("Can't proceed input in: " + buildUrl + " with id: " + inputId, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }
    }

    /**
     * Represents the results of a build.
     */
    class Build {
        Result result
        String number

        Build(Map args) {
            this.result = args.result
            this.number = args.number
        }
    }

    /**
     * Triggers a build for a specified Jenkins item and waits for its completion.
     *
     * @param itemName in human-readable format
     * @param inputParams optional, parameters required for triggering the build. Example: [[name: "SOME_STRING_PARAM", body: "some_string"], ...]
     * @return {@link Build} object
     * @throws {@code java.util.concurrent.TimeoutException} if the build does not complete within 1 hour
     */
    Build build(String itemName, List inputParams = null) {
        def response = this.buildRequest(itemName, inputParams)

        String itemQueueUrl = response.headers.get("Location").get(0)

        String buildNumber = this.pollBuildNumber(itemQueueUrl)
        Result result = this.pollBuildResult(itemName, buildNumber)

        return new Build(result: result, number: buildNumber)
    }

    /**
     * Triggers a build for a specified Jenkins item with input-step and waits for its completion.
     *
     * @param itemName in human-readable format
     * @param inputParams optional, parameters required for triggering the build. Example: [[name: "SOME_STRING_PARAM", body: "some_string"], ...]
     * @param inputId custom input ID
     * @return {@link Build} object
     * @throws {@code java.util.concurrent.TimeoutException} if the build does not complete within 1 hour
     */
    Build buildWithInputRequest(String itemName, List inputParams = null, String inputId) {
        def response = this.buildRequest(itemName, inputParams)

        String itemQueueUrl = response.headers.get("Location").get(0)
        String buildNumber = this.pollBuildNumber(itemQueueUrl)

        this.pollInputAction(itemName, buildNumber)
        this.simpleProceed(this.url + itemNameToUrl(itemName) + '/' + buildNumber, inputId)
        Result result = this.pollBuildResult(itemName, buildNumber)

        return new Build(result: result, number: buildNumber)
    }

    private def buildRequest(String itemName, List inputParams = null) {
        String path = this.itemNameToUrl(itemName)
        String buildUrl = inputParams ? "buildWithParameters" : "build"

        def response = new HttpRequest(
                url: this.url + path + "/${buildUrl}?delay=0sec",
                auth: this.credential
        ).post(inputParams ?: "")

        if (response.status != 201 && response.status != 303) {
            this.steps.log("Failure when building Jenkins Item: " + itemName + ", with status: " + response.status, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }
        return response
    }

    private String pollBuildNumber(String itemQueueUrl) {
        return Poller.poll({
            def api = this.getApiXml(itemQueueUrl)
            def xml = new XmlSlurper().parseText(api)
            String number = xml.executable.number.text()
            return number ?: null
        })
    }

    private Result pollBuildResult(String itemName, String buildNumber, int timeoutSeconds = 3600) {
        String result = Poller.poll({
            def api = this.getBuildApiXml(itemName, buildNumber)
            def xml = new XmlSlurper().parseText(api)
            if (Boolean.valueOf(xml.building.text())) {
                return null
            }
            return xml.result.text()
        }, null, 10, timeoutSeconds)
        return Result.valueOfResult(result)
    }

    private void pollInputAction(String itemName, String buildNumber) {
        Poller.poll({
            def api = this.getBuildApiXml(itemName, buildNumber)
            def xml = new XmlSlurper().parseText(api)
            if (!Boolean.valueOf(xml.building.text())) {
                throw new InputRequestNotFoundException("Job " + itemName + "/" + buildNumber + " terminated without input request")
            }
            if (xml.action.any { it['@_class'] == 'org.jenkinsci.plugins.workflow.support.steps.input.InputAction' }) {
                return "Input found"
            }
            return null
        })
    }

    private String itemNameToUrl(String itemName) {
        itemName?.tokenize('/').collect { it -> return "job/$it" }.join('/')
    }
}
