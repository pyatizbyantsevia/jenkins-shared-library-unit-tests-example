package mainpackage.api

import mainpackage.LogLevel
import mainpackage.http.HttpRequest

class BitBucketAPI {

    private String bitBucketURL
    private String httpCred
    private Script steps

    BitBucketAPI(String bitBucketURL, String httpCred, Script steps) {
        this.steps = steps
        this.bitBucketURL = bitBucketURL
        this.httpCred = httpCred
    }

    String getSSHLink(String repoName) {
        def headers = [
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.bitBucketURL + "repos/" + repoName,
                auth: this.httpCred,
                steps: this.steps
        )
                .get()

        if (response.status != 200) {
            steps.log("Не удалось получить сведения о репозитории: " + repoName, LogLevel.ERROR)
            steps.error "${response.content}"
        }

        def json = new groovy.json.JsonSlurper().parseText(response.content)
        def sshRef = json.links.clone.find { it.name == 'ssh' }?.href

        return sshRef
    }

    void createRepository(String repoName) {

        def body = "{\"name\":\"${repoName}\",\"scmId\":\"git\"}"
        def headers = [
                [name: 'Content-Type', value: 'application/json'],
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.bitBucketURL + "repos/",
                auth: this.httpCred,
                steps: this.steps
        )
                .post(body)

        if (response.status != 201) {
            steps.log("Не удалось создать репозиторий в BitBucket с именем: " + repoName, LogLevel.ERROR)
            steps.error "${response.content}"
        }
    }

    Boolean isRepositoryExist(String repoName) {

        def headers = [
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.bitBucketURL + "repos/" + repoName,
                auth: this.httpCred,
                steps: this.steps
        )
                .get()

        if (response.status == 404) {
            return false
        } else if (response.status == 200) {
            return true
        } else {
            steps.log("Не удалось получить сведения о репозитории: " + repoName, LogLevel.ERROR)
            steps.error "${response.content}"
        }
    }

    Boolean isBranchExist(String repoName, String branchToCheck) {

        def headers = [
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.bitBucketURL + "repos/" + repoName + "/branches",
                auth: this.httpCred,
                steps: this.steps
        )
                .get()

        if (response.status != 200) {
            steps.log("Не удалось получить сведения о ветке: " + branchToCheck + " в репозитории: " + repoName, LogLevel.ERROR)
            steps.error "${response.content}"
        }

        def json = new groovy.json.JsonSlurper().parseText(response.content)
        boolean isBranchToCheckExist = json.values.any { it.displayId == branchToCheck }

        return isBranchToCheckExist
    }

}
