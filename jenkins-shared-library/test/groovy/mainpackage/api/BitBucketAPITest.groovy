package mainpackage.api

import mainpackage.LogLevel
import mainpackage.PipelineContext
import mainpackage.exception.UnexpectedResponseCodeException
import mainpackage.mock.StepsMock
import spock.lang.Specification


class BitBucketAPITest extends Specification{

    def testSteps = Mock(StepsMock)
    BitBucketAPI BBApi

    def setup() {
        PipelineContext.init(testSteps)
        testSteps.withRetry(_ as Closure) >> { Closure body -> body.call() }
        BBApi = new BitBucketAPI(url: 'http://bitbucket.com/', credential: 'token')
    }

    def "createRepository: expecting 201 code - Success"() {
        def mockResponse = [status: 201, content: 'Posted']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        BBApi.createRepository('test-repo-name')

        then:
        noExceptionThrown()
    }

    def "createRepository: expecting 404 code - Fail"() {
        def mockResponse = [status: 404, content: 'Posted']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        BBApi.createRepository('test-repo-name')

        then:
        1 * testSteps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "isRepositoryExists: expecting 201 code - Success"() {
        def mockResponse = [status: 200, content: 'Get']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        Boolean response = BBApi.isRepositoryExist('test-repo-name')

        then:
        response
    }

    def "isRepositoryExists: expecting 404 code - Fail"() {
        def mockResponse = [status: 404, content: 'Get']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        Boolean response = BBApi.isRepositoryExist('test-repo-name')

        then:
        !response
    }

    def "isRepositoryExists: unexpected *** code - Fail"() {
        def mockResponse = [status: 321, content: 'Posted']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        BBApi.isRepositoryExist('test-repo-name')

        then:
        1 * testSteps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "getSSHLink: unexpected *** code - Fail"() {
        def mockResponse = [status: 321, content: 'Posted']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        BBApi.getSSHLink('test-repo-name')

        then:
        1 * testSteps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "getSSHLink: searching ssh://git@bitbucket:/project/shared-library.git - True"() {
        def mockResponse = [status: 200, content: new String(this.getClass().getResource('get-ssh.json').bytes)]

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        String response = BBApi.getSSHLink('shared-library')

        then:
        response == 'ssh://git@bitbucket:/project/shared-library.git'
    }

    def "isBranchExists: unexpected *** code - Fail"() {
        def mockResponse = [status: 321, content: 'Posted']

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        BBApi.isBranchExist('test-repo-name', 'test')

        then:
        1 * testSteps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "isBranchExists: searching test/BitBacketAPI, redesign-jenkinsapi, master - True"() {
        def mockResponse = [status: 200, content: new String(this.getClass().getResource('branches-exist.json').bytes)]

        given:
        testSteps.httpRequest(_ as Map) >> mockResponse

        when:
        Boolean response = BBApi.isBranchExist('shared-library', 'test/BitBacketAPI')
        Boolean response2 = BBApi.isBranchExist('shared-library', 'redesign-jenkinsapi')
        Boolean response3 = BBApi.isBranchExist('shared-library', 'master')

        then:
        response && response2 && response3
    }
}