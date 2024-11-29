package api

import LogLevel
import http.HttpRequest

import groovy.util.XmlSlurper

public class JenkinsAPI {

    private String jenkinsURL
    private String jenkinsToken
    private Script steps

    public JenkinsAPI(String jenkinsURL, String jenkinsToken, Script steps) {
        this.jenkinsURL = jenkinsURL
        this.jenkinsToken = jenkinsToken
        this.steps = steps
    }

    public void waitJobComplete(String jobURL) {
        steps.log("Ожидание успешной сборки : " + jobURL + " ", LogLevel.INFO)

        def time = 3600000
        long startTime = System.currentTimeMillis()
        def isBuilding = true
        def status = ""

        while (isBuilding) {
            steps.sleep(15)

            def response = getApiXml(jobURL)
            def xml = new XmlSlurper().parseText(response.content)

            isBuilding = Boolean.valueOf(xml.building.text())
            status = xml.result.text()

            long elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime >= time) {
                steps.error("TimeOut")
            }

            ifInputRequired(jobURL)
        }
        if ((status == "FAILURE") || (status == "UNSTABLE")) {
            steps.error("Failed To run job " + status + " : " + jobURL)
        }
    }

    public void createFolder(String folderName) {
        def headers = [
                        [name: 'Content-Type', value: 'application/json']
                      ]

        def response = new HttpRequest(
                            headers: headers,
                            url: this.jenkinsURL + 'createItem?name=' + folderName + '&mode=com.cloudbees.hudson.plugins.folder.Folder&Submit=OK',
                            auth: this.jenkinsToken,
                            desiredResponseCode: '200:404',
                            steps: this.steps
                        )
                        .post("{}")

        if ((response.status == 302) || (response.status == 400)) {
            steps.log("Jenkins Folder создан или существовал: " + this.jenkinsURL + "job/" + folderName + " ", LogLevel.INFO)
        } else {
            steps.log("Не удалось создать jenkins folder: " + jenkinsURL + "job/" + folderName + " ", LogLevel.ERROR)
            steps.error(response.content)
        }
    }

    public void createJob(String folderName, String jobName) {
        def headers = [
                        [name: 'Content-Type', value: 'application/json']
                      ]

        def response = new HttpRequest(
                            headers: headers,
                            url: this.jenkinsURL + "job/" + folderName + '/createItem?name=' + jobName + '&mode=org.jenkinsci.plugins.workflow.job.WorkflowJob',
                            auth: this.jenkinsToken,
                            desiredResponseCode: '200:404',
                            steps: this.steps
                        )
                        .post("{}")

        if ((response.status == 302) || (response.status == 400)) {
            steps.log("Jenkins Job создан или существовал: " + this.jenkinsURL + "job/" + folderName + "/job/" + jobName + " ", LogLevel.INFO)
        } else {
            steps.log(response.status + "Не удалось создать jenkins job: " + jobName + " ", LogLevel.ERROR)
            steps.error(response.content)
        }
    }

    public void startJob(String jobUrl, String inputParams) {
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

}
