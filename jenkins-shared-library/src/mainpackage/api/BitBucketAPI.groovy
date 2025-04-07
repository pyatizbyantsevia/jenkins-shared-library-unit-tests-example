package mainpackage.api

import mainpackage.LogLevel
import mainpackage.PipelineContext
import mainpackage.exception.UnexpectedResponseCodeException
import mainpackage.http.HttpRequest

/**
 * Interacts with BitBucket through its REST API.
 *
 * Provides methods for managing repositories and branches.
 */
class BitBucketAPI extends API {

    private Script steps = PipelineContext.instance.steps

    /**
     * @param args named parameters.
     *     <ul>
     *       <li>{@code url} - BitBucket server with specified project (example: http://bitbucket/projects/ProjectName/)</li>
     *       <li>{@code credential} - The authentication Jenkins credential, kind: username with password</li>
     *     </ul>
     *     If any of these parameters are missing, an {@link IllegalArgumentException} will be thrown
     */
    BitBucketAPI(Map args) {
        super(args)
    }

    /**
     * Retrieves the SSH link for the specified repository.
     * @param repoName
     * @return SSH link to the repository, or null if not found
     * @throws UnexpectedResponseCodeException if the server returns an unexpected response code
     */
    String getSSHLink(String repoName) {
        def headers = [
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.url + "repos/" + repoName,
                auth: this.credential
        ).get()

        if (response.status != 200) {
            this.steps.log("Can't retrieve repository data: " + repoName, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
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
                url: this.url + "repos/",
                auth: this.credential
        ).post(body)

        if (response.status != 201) {
            this.steps.log("Can't create repository: " + repoName, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }
    }

    Boolean isRepositoryExist(String repoName) {
        def headers = [
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.url + "repos/" + repoName,
                auth: this.credential
        ).get()

        if (response.status == 404) {
            return false
        } else if (response.status == 200) {
            return true
        } else {
            this.steps.log("Can't retrieve repository data: " + repoName, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }
    }

    Boolean isBranchExist(String repoName, String branchToCheck) {
        def headers = [
                [name: 'Accept', value: 'application/json']
        ]

        def response = new HttpRequest(
                headers: headers,
                url: this.url + "repos/" + repoName + "/branches",
                auth: this.credential
        ).get()

        if (response.status != 200) {
            this.steps.log("Can't retrieve branch info: " + branchToCheck + " in repository: " + repoName, LogLevel.ERROR)
            throw new UnexpectedResponseCodeException(response.content, response.status)
        }

        def json = new groovy.json.JsonSlurper().parseText(response.content)
        return json.values.any { it.displayId == branchToCheck }
    }

}
