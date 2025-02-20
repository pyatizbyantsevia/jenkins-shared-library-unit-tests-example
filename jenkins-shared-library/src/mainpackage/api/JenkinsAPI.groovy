package mainpackage.api

import mainpackage.LogLevel
import mainpackage.exception.UnexpectedResponseCodeException
import mainpackage.http.HttpRequest
import mainpackage.exception.BuildFailureException

class JenkinsAPI {

    private String jenkinsURL
    private String jenkinsToken
    private Script steps

    JenkinsAPI(String jenkinsURL, String jenkinsToken, Script steps) {
        if (!jenkinsURL || !jenkinsToken || !steps) {
            throw new IllegalArgumentException("When creating a JenkinsApi object, you must pass the jenkinsURL, jenkinsToken and steps parameters")
        }

        this.jenkinsURL = jenkinsURL
        this.jenkinsToken = jenkinsToken
        this.steps = steps
    }

    void waitJobComplete(String jobURL) {
        def time = 3600000
        long startTime = System.currentTimeMillis()
        def isBuilding = true
        def status = ""

        while (isBuilding) {
            steps.log("Ожидание успешной сборки : " + jobURL + " ", LogLevel.NOTICE)
            steps.sleep(15)

            def response = this.getApiXml(jobURL)
            def xml = new XmlSlurper().parseText(response.content)

            isBuilding = Boolean.valueOf(xml.building.text())
            status = xml.result.text()

            long elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime >= time) {
                steps.error("TimeOut")
            }
        }
        if ((status == "FAILURE")) {
            throw new BuildFailureException("Failed To run job " + status + " : " + jobURL)
        }
    }

    void createJob(String jobName, String xml = steps.libraryResource('mainpackage/api/simple-job.xml')) {
        this.createItem(jobName, xml)
    }

    void createFolder(String folderName, String xml = steps.libraryResource('mainpackage/api/simple-folder.xml')) {
        this.createItem(folderName, xml)
    }

    /**
     * @param itemName, example: rootFolder/folderToCreate; rootFolder/jobToCreate
     * @param xml, it defines your item
     */
    void createItem(String itemName, String xml) {
        def headers = [
                [name: 'Content-Type', value: 'application/xml']
        ]

        String itemToCreate = itemName.tokenize('/').last()
        String path = this.itemNameToUrl(itemName.tokenize('/').dropRight(1).join('/'))

        def response = new HttpRequest(
                headers: headers,
                url: this.jenkinsURL + path + '/createItem?name=' + itemToCreate,
                auth: this.jenkinsToken,
                steps: this.steps
        ).post(xml)

        if (response.status == 400) {
            steps.log("Jenkins Item already exist: " + itemName, LogLevel.NOTICE)
        } else if (response.status != 200) {
            steps.log("Failure when creating Jenkins Item: " + itemName + ", with status: " + response.status, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content)
        }
    }

    /**
     * @param itemName in human-readable format, example: rootFolder/folderToUpdate; rootFolder/jobToUpdate
     * @param xml, it defines your update
     */
    void updateItem(String itemName, String xml) {
        def headers = [
                [name: 'Content-Type', value: 'application/xml']
        ]

        String path = this.itemNameToUrl(itemName)

        def response = new HttpRequest(
                headers: headers,
                url: jenkinsURL + path + "/config.xml",
                auth: jenkinsToken,
                steps: steps
        ).post(xml)

        if (response.status != 200) {
            steps.log("Failure when updating Jenkins Item: " + itemName + ", with status: " + response.status, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content)
        }
    }

    void startJob(String jobUrl, String inputParams) {
        def headers = [
                [name: 'Content-Type', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: jobUrl + "/buildWithParameters?delay=0sec" + inputParams,
                auth: this.jenkinsToken,
                steps: this.steps
        )
                .post("{}")

        steps.log("Запущена сборка, статус:" + response.status, LogLevel.INFO)
    }

    void reconfigJob(String jobPath) {
        def headers = [
                [name: 'Content-Type', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url:  this.jenkinsURL + jobPath + "/build?delay=0sec",
                auth: this.jenkinsToken,
                steps: this.steps
        ).post("{}")
        steps.log("Запущен реконфиг джобы:" + this.jenkinsURL + jobPath  + " ", LogLevel.INFO)
    }

    String getLastBuildNumber(String jobURL) {
        steps.sleep(5)

        def response = this.getApiXml(jobURL)
        def xml = new XmlSlurper().parseText(response.content)

        return xml.lastBuild.number.text()
    }

    def getApiXml(String itemName, String buildNumber = '') {
        String path = this.itemNameToUrl(itemName)

        def response = new HttpRequest(
                url: jenkinsURL + path + "/${buildNumber}/api/xml",
                auth: jenkinsToken,
                steps: steps
        ).get()

        if (response.status != 200) {
            steps.log("Failed to get job information: " + itemName, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content)
        }

        return response.content
    }

    private String itemNameToUrl(String itemName) {
        itemName.tokenize('/').collect {it -> return "job/$it"}.join('/')
    }
}
