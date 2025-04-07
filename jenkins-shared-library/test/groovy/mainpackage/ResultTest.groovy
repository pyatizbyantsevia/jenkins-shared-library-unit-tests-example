package mainpackage

import spock.lang.Specification

class ResultTest extends Specification {

    def "valueOfResult: convert String to Result"() {
        given:
        String str = "ABORTED"

        when:
        Result result = Result.valueOfResult(str)

        then:
        result == Result.ABORTED
    }

    def "valueOfResult: value not found"() {
        given:
        String str = "ABORTEDDDDD"

        when:
        Result result = Result.valueOfResult(str)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "No enum constant mainpackage.Result.ABORTEDDDDD"
    }
}
