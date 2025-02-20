package mainpackage.api

import mainpackage.LogLevel
import mainpackage.exception.UnexpectedResponseCodeException
import mainpackage.mock.MockSteps

import spock.lang.Specification
import java.lang.reflect.Method

import static org.junit.Assert.*;

class JenkinsAPITest extends Specification {

    def steps = Mock(MockSteps)
    JenkinsAPI jenkinsAPI

    def setup() {
        jenkinsAPI = new JenkinsAPI('http://jenkins-stub/', 'token', steps)
    }

    def "getApiXml: return api/xml if return-code 200"() {
        def mockResponse = [status: 200, content: '<xml>content</xml>']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        def response = jenkinsAPI.getApiXml('some-item')

        then:
        noExceptionThrown()
        assertEquals(mockResponse.content, response)
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
        e.message == mockResponse.content
    }

    def "itemNameToUrl: return job-separated url, 3 length"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "rootFolder/subFolder/pipeline")

        then:
        assertEquals("job/rootFolder/job/subFolder/job/pipeline", result)
    }

    def "itemNameToUrl: return job-separated url, 1 length"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "rootFolder")

        then:
        assertEquals("job/rootFolder", result)
    }

    def "itemNameToUrl: return job-separated url, empty"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "")

        then:
        assertEquals("", result)
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

    def "createFolder: use user value"() {
        def mockResponse = [status: 400, content: 'Existed']

        given:
        steps.httpRequest(_) >> mockResponse

        when:
        jenkinsAPI.createFolder('rootFolder/folderToCreate', "aboba")

        then:
        1 * steps.log(_, LogLevel.NOTICE)
        noExceptionThrown()
    }


}
