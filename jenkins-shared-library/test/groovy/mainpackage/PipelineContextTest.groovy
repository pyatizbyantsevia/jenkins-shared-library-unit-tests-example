package mainpackage

import mainpackage.mock.StepsMock
import spock.lang.Specification

class PipelineContextTest extends Specification {


    def "PipelineContext: get steps without init"() {
        when:
        PipelineContext.getInstance().getSteps()

        then:
        thrown(RuntimeException)
    }

    def "PipelineContext: get steps with init"() {
        given:
        def stepsMock = Mock(StepsMock)

        when:
        PipelineContext.init(stepsMock)
        def steps= PipelineContext.getInstance().getSteps()

        then:
        noExceptionThrown()
    }
}
