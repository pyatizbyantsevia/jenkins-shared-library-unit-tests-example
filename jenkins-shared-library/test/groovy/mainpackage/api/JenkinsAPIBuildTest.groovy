package mainpackage.api

import mainpackage.LogLevel
import mainpackage.PipelineContext
import mainpackage.Result
import mainpackage.exception.UnexpectedResponseCodeException
import mainpackage.exception.InputRequestNotFoundException
import mainpackage.http.HttpRequest
import mainpackage.mock.StepsMock
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeoutException

class JenkinsAPIBuildTest extends Specification {

    def steps = Mock(StepsMock)
    JenkinsAPI jenkinsAPI

    def setup() {
        PipelineContext.init(steps)

        steps.withRetry(_) >> { Closure body -> body.call() }
        jenkinsAPI = new JenkinsAPI(url: 'http://jenkins-stub/', credential: 'token', steps: steps)
    }

    def "pollBuildResult: result build is aborted"() {
        def mockResponse = [status: 200, content: new String(this.getClass().getResource('build-api-xml-with-building-false.xml').bytes)]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        Result result = jenkinsAPI.pollBuildResult('rootFolder/jobToGetBuildResult', "1")

        then:
        result == Result.ABORTED
    }

    def "pollBuildResult: timeout"() {
        def mockResponse = [status: 200, content: new String(this.getClass().getResource('build-api-xml-with-building-true.xml').bytes)]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.pollBuildResult("rootFolder/jobToGetBuildResult", "1",2)

        then:
        thrown(TimeoutException)
    }

    def "pollBuildNumber: get build number"() {
        def mockResponse = [status: 200, content: new String(this.getClass().getResource('queue-item-api-xml.xml').bytes)]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        String number = jenkinsAPI.pollBuildNumber("http://jenkins-stub/queue/item/1477/")

        then:
        number == "60"
    }

    @Ignore
    def "pollBuildNumber: timeout"() {
        def mockResponse = [status: 200, content: new String(this.getClass().getResource('queue-item-api-xml-without-executable.xml').bytes)]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.pollBuildNumber("http://jenkins-stub/queue/item/1477/")

        then:
        thrown(TimeoutException)
    }

    def "buildRequest: build without params"() {
        def mockResponse = [status: 201, headers: ["Location": "http://jenkins-stub/queue/item/1477/"]]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        def response = jenkinsAPI.buildRequest("rootFolder/jobToBuild")

        then:
        response.headers.get("Location") == "http://jenkins-stub/queue/item/1477/"
    }

    def "buildRequest: build with params"() {
        def mockResponse = [status: 201, headers: ["Location": "http://jenkins-stub/queue/item/1477/"]]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        def response = jenkinsAPI.buildRequest("rootFolder/jobToBuild", [[name: "STRING_PARAM", body: "temp"], [name: "BOOLEAN_PARAM", body: false]])

        then:
        response.headers.get("Location") == "http://jenkins-stub/queue/item/1477/"
    }

    def "buildRequest: build when item already in queue"() {
        def mockResponse = [status: 303, headers: ["Location": "http://jenkins-stub/queue/item/1477/"]]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        def response = jenkinsAPI.buildRequest("rootFolder/jobToBuild", [[name: "STRING_PARAM", body: "temp"], [name: "BOOLEAN_PARAM", body: false]])

        then:
        response.headers.get("Location") == "http://jenkins-stub/queue/item/1477/"
    }

    def "buildRequest: wrong request"() {
        def mockResponse = [status: 401, content: "Not authorized"]

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        def response = jenkinsAPI.buildRequest("rootFolder/jobToBuild")

        then:
        1 * steps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    @Unroll
    def "test buildRequest with various status codes: code #status"() {
        given:
        def mockResponse = [status: status, content: "content"]

        def mockHttpRequest = Mock(HttpRequest)
        GroovyMock(HttpRequest, global: true)
        new HttpRequest(_) >> mockHttpRequest
        mockHttpRequest.post(_) >> mockResponse

        when:
        def exception = null
        def result = null
        try {
            result = jenkinsAPI.buildRequest("itemName")
        } catch (Exception e) {
            exception = e
        }

        then:
        if (successExpected) {
            assert (result == mockResponse)
        } else {
            assert (result == null)
            assert exception instanceof UnexpectedResponseCodeException
        }

        where:
        status | successExpected
        201    | true
        303    | true
        200    | false
        202    | false
        204    | false
        400    | false
        401    | false
        403    | false
        404    | false
        500    | false
    }

    def "build: wait success build"() {
        given:
        def buildRequest = Mock(HttpRequest)
        def pollBuildNumber = Mock(HttpRequest)
        def pollBuildResult = Mock(HttpRequest)
        GroovyMock(HttpRequest, global: true)

        new HttpRequest({Map args -> args.url.contains("build")}) >> buildRequest
        new HttpRequest({Map args -> args.url.contains("http://jenkins-stub/queue/item/1477/")}) >> pollBuildNumber
        new HttpRequest({Map args -> args.url.contains("rootFolder/job/jobToBuild/60")}) >> pollBuildResult

        and:
        buildRequest.post(_) >> [status: 201, headers: ["Location": ["http://jenkins-stub/queue/item/1477/"]]]
        pollBuildNumber.get() >> [status: 200, content: new String(this.getClass().getResource('queue-item-api-xml.xml').bytes)]
        pollBuildResult.get() >> [status: 200, content: new String(this.getClass().getResource('build-api-xml-60.xml').bytes)]

        when:
        def build = jenkinsAPI.build("rootFolder/jobToBuild")

        then:
        build.number == "60"
        build.result == Result.SUCCESS
    }

    def "buildWithInputRequest: terminated without input request"() {
        given:
        def buildRequest = Mock(HttpRequest)
        def pollBuildNumber = Mock(HttpRequest)
        def pollInputAction = Mock(HttpRequest)
        GroovyMock(HttpRequest, global: true)

        new HttpRequest({Map args -> args.url.contains("build")}) >> buildRequest
        new HttpRequest({Map args -> args.url.contains("http://jenkins-stub/queue/item/15090/")}) >> pollBuildNumber
        new HttpRequest({Map args -> args.url.contains("rootFolder/job/jobToBuild/122")}) >> pollInputAction

        and:
        buildRequest.post(_) >> [status: 201, headers: ["Location": ["http://jenkins-stub/queue/item/15090/"]]]
        pollBuildNumber.get() >> [status: 200, content: new String(this.getClass().getResource('queue-item-api-xml-15090.xml').bytes)]
        pollInputAction.get() >> [status: 200, content: new String(this.getClass().getResource('build-api-xml-122.xml').bytes)]

        when:
        def build = jenkinsAPI.buildWithInputRequest("rootFolder/jobToBuild", null, "INPUT_ID")

        then:
        thrown(InputRequestNotFoundException)
    }

    def "buildWithInputRequest: success"() {
        given:
        def buildRequest = Mock(HttpRequest)
        def pollBuildNumber = Mock(HttpRequest)
        def simpleProceed = Mock(HttpRequest)
        def pollInputAction = Mock(HttpRequest)
        def pollBuildResult = Mock(HttpRequest)
        GroovyMock(HttpRequest, global: true)

        new HttpRequest({Map args -> args.url.contains("build")}) >> buildRequest
        new HttpRequest({Map args -> args.url.contains("http://jenkins-stub/queue/item/15152/")}) >> pollBuildNumber
        new HttpRequest({Map args -> args.url.contains("/wfapi/inputSubmit?inputId=INPUT_ID")}) >> simpleProceed
        new HttpRequest({Map args -> args.url.contains("rootFolder/job/jobToBuild/30")}) >> pollInputAction >> pollBuildResult

        and:
        buildRequest.post(_) >> [status: 201, headers: ["Location": ["http://jenkins-stub/queue/item/15152/"]]]
        pollBuildNumber.get() >> [status: 200, content: new String(this.getClass().getResource('queue-item-api-xml-15152.xml').bytes)]
        simpleProceed.post(_) >> [status: 200, content: "Proceed successful"]
        pollInputAction.get() >> [status: 200, content: new String(this.getClass().getResource('build-api-xml-30.xml').bytes)]
        pollBuildResult.get() >> [status: 200, content: new String(this.getClass().getResource('build-api-xml-30-finished.xml').bytes)]

        when:
        def build = jenkinsAPI.buildWithInputRequest("rootFolder/jobToBuild", null, "INPUT_ID")

        then:
        build.result == Result.UNSTABLE
    }
}
