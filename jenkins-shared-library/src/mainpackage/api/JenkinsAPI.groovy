package mainpackage.api

import mainpackage.LogLevel
import mainpackage.http.HttpRequest

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
        steps.log('Ожидание успешной сборки : ' + jobURL + ' ', LogLevel.INFO)

        def time = 3600000
        long startTime = System.currentTimeMillis()
        def isBuilding = true
        def status = ''

        while (isBuilding) {
            steps.sleep(15)

            def response = getApiXml(jobURL)
            def xml = new XmlSlurper().parseText(response.content)

            isBuilding = Boolean.valueOf(xml.building.text())
            status = xml.result.text()

            long elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime >= time) {
                steps.error('TimeOut')
            }

            ifInputRequired(jobURL)
        }
        if ((status == 'FAILURE') || (status == 'UNSTABLE')) {
            steps.error('Failed To run job ' + status + ' : ' + jobURL)
        }
    }

    /**
    * @param folderName, example: rootFolder/folderToCreate
    * @param xml, define your folder
    */
    public void createFolder(String folderName, String xml) {
        def headers = [
                        [name: 'Content-Type', value: 'application/xml']
                      ]

        String folderToCreate = folderName.tokenize('/').last()
        String path = folderName.tokenize('/').dropRight(1).collect { it -> return "job/$it" }.join('/')

        def response = new HttpRequest(
                            headers: headers,
                            url: this.jenkinsURL + path + 'createItem?name=' + folderToCreate,
                            auth: this.jenkinsToken,
                            desiredResponseCode: '200:404',
                            steps: this.steps
                        )
                        .post(xml)

        steps.log('LOG:' + response.status, LogLevel.ERROR)
        steps.log('LOG2:' + response.content, LogLevel.ERROR)
        steps.log('LOG2:' + xml, LogLevel.ERROR)

        if (response.status == 404) {
            steps.log('Wrong path to create Jenkins Folder: ' + folderName, LogLevel.ERROR)
            steps.error(response.content)
        } else if (response.status == 400) {
            steps.log('Jenkins Folder already exist: ' + folderName, LogLevel.NOTICE)
        } else if (response.status != 200) {
            steps.log('Failure when creating Jenkins Folder: ' + folderName, LogLevel.ERROR)
            steps.error(response.content)
        }
    }

    public void createJob(String folderName, String jobName) {
        def headers = [
                        [name: 'Content-Type', value: 'application/json']
                      ]

        def response = new HttpRequest(
                            headers: headers,
                            url: this.jenkinsURL + 'job/' + folderName + '/createItem?name=' + jobName + '&mode=org.jenkinsci.plugins.workflow.job.WorkflowJob',
                            auth: this.jenkinsToken,
                            desiredResponseCode: '200:404',
                            steps: this.steps
                        )
                        .post('{}')

        if ((response.status == 302) || (response.status == 400)) {
            steps.log('Jenkins Job создан или существовал: ' + this.jenkinsURL + 'job/' + folderName + '/job/' + jobName + ' ', LogLevel.INFO)
        } else {
            steps.log(response.status + 'Не удалось создать jenkins job: ' + jobName + ' ', LogLevel.ERROR)
            steps.error(response.content)
        }
    }

    public void startJob(String jobUrl, String inputParams) {
        def headers = [
                        [name: 'Content-Type', value: 'application/json']
                      ]

        def response = new HttpRequest(
                            headers: headers,
                            url: jobUrl + '/buildWithParameters?delay=0sec' + inputParams,
                            auth: this.jenkinsToken,
                            steps: this.steps
                        )
                        .post('{}')

        steps.log('Запущена сборка, статус:' + response.status, LogLevel.INFO)
    }

}
