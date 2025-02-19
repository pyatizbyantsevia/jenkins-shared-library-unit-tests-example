package mainpackage.api

import mainpackage.LogLevel
import mainpackage.exception.BuildFailureException
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

    def "getApiXml: return api/xml if return-code == 200"() {
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
        def e = thrown(BuildFailureException)
        e.message == mockResponse.content
    }

    def "itemNameToUrl: return job-separated url"() {
        Method method = JenkinsAPI.getDeclaredMethod("itemNameToUrl", String.class)
        method.setAccessible(true)

        when:
        String result = (String) method.invoke(jenkinsAPI, "rootFolder/subFolder/pipeline")

        then:
        assertEquals("job/rootFolder/job/subFolder/job/pipeline", result)
    }
}
