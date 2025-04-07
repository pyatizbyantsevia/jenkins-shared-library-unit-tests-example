package mainpackage.api

import mainpackage.PipelineContext
import mainpackage.mock.StepsMock
import mainpackage.LogLevel
import mainpackage.exception.UnexpectedResponseCodeException
import spock.lang.Specification
import java.lang.reflect.Method

class JenkinsAPITest extends Specification {

    def steps = Mock(StepsMock)
    JenkinsAPI jenkinsAPI

    def setup() {
        PipelineContext.init(steps)

        steps.withRetry(_) >> { Closure body -> body.call() }
        jenkinsAPI = new JenkinsAPI(url: 'http://jenkins-stub/', credential: 'token')
    }

    def "constructor: throw exception if one of parameters null"() {
        given:
        String jenkinsUrl = "stub"
        String jenkinsToken = null

        when:
        new JenkinsAPI(url: jenkinsUrl, credential: jenkinsToken)

        then:
        thrown(IllegalArgumentException)
    }

    def "constructor: throw exception if one of parameters empty"() {
        given:
        String jenkinsUrl = "stub"
        String jenkinsToken = ""

        when:
        new JenkinsAPI(url: jenkinsUrl, credential: jenkinsToken)

        then:
        thrown(IllegalArgumentException)
    }

    def "constructor: throw exception if all parameters null"() {
        given:
        String jenkinsUrl
        String jenkinsToken

        when:
        new JenkinsAPI(url: jenkinsUrl, credential: jenkinsToken)

        then:
        thrown(IllegalArgumentException)
    }

    def "getApiXml: return api/xml if return-code 200"() {
        def mockResponse = [status: 200, content: '<xml>content</xml>']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        def response = jenkinsAPI.getApiXml('some-item')

        then:
        noExceptionThrown()
        mockResponse.content == response
    }

    def "getApiXml: throw exception if return-code != 200"() {
        def mockResponse = [status: 404, content: '<xml>Failure</xml>']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.getApiXml('some-item')

        then:
        1 * steps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.unexpectedResponseCode == 404
        e.message == mockResponse.content
    }

    def "itemNameToUrl: return job-separated url, 3 length"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "rootFolder/subFolder/pipeline")

        then:
        "job/rootFolder/job/subFolder/job/pipeline" == result
    }

    def "itemNameToUrl: return job-separated url, 1 length"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "rootFolder")

        then:
        "job/rootFolder" == result
    }

    def "itemNameToUrl: return job-separated url, empty"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "")

        then:
        "" == result
    }

    def "updateItem: throw exception if return-code != 200"() {
        def mockResponse = [status: 404, content: 'Failure: Not found']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.updateItem('some-item', '<xml>item</xml>')

        then:
        1 * steps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "updateItem: update item if return-code 200"() {
        def mockResponse = [status: 200, content: 'Success']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.updateItem('some-item', '<xml>item</xml>')

        then:
        noExceptionThrown()
    }

    def "updateItem: empty item"() {
        def mockResponse = [status: 404, content: 'Failure: Not found']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.updateItem(null, '<xml>item</xml>')

        then:
        1 * steps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "createItem: create item if return-code 200"() {
        def mockResponse = [status: 200, content: 'Success']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.createItem('rootFolder/folderToCreate', '<xml>item</xml>')

        then:
        noExceptionThrown()
    }

    def "createItem: log if return-code 400"() {
        def mockResponse = [status: 400, content: 'Existed']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.createItem('rootFolder/folderToCreate', '<xml>item</xml>')

        then:
        1 * steps.log(_, LogLevel.NOTICE)
        noExceptionThrown()
    }

    def "createItem: throw exception if return-code != 400,200"() {
        def mockResponse = [status: 404, content: 'Not Found']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.createItem('rootFolder/folderToCreate', '<xml>item</xml>')

        then:
        1 * steps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "createFolder: use default xml value"() {
        def mockResponse = [status: 400, content: 'Existed']

        given:
        steps.httpRequest(_) >> mockResponse
        steps.libraryResource(_) >> { new String(this.getClass().getResource('simple-folder.xml').bytes) }

        when:
        jenkinsAPI.createFolder('rootFolder/folderToCreate')

        then:
        1 * steps.log(_, LogLevel.NOTICE)
        noExceptionThrown()
    }

    def "createJob: create simple job"() {
        def mockResponse = [status: 400, content: 'Existed']

        given:
        steps.httpRequest(_) >> mockResponse
        steps.libraryResource(_) >> { new String(this.getClass().getResource('simple-job.xml').bytes) }

        when:
        jenkinsAPI.createJob('rootFolder/jobToCreate')

        then:
        1 * steps.log(_, LogLevel.NOTICE)
        noExceptionThrown()
    }

    def "simpleProceed: throw exception if return-code != 200"() {
        def mockResponse = [status: 400, content: 'Wrong!']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.simpleProceed('rootFolder/jobToInput', "INPUT_ID")

        then:
        1 * steps.log(_, LogLevel.ERROR)
        def e = thrown(UnexpectedResponseCodeException)
        e.message == mockResponse.content
    }

    def "simpleProceed: success"() {
        def mockResponse = [status: 200, content: 'Success']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.simpleProceed('rootFolder/jobToInput', "INPUT_ID")

        then:
        noExceptionThrown()
    }
}