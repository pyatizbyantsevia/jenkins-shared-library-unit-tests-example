package mainpackage.mock

import mainpackage.LogLevel

class StepsMock extends Script {

    @Override
    Object run() {
        return null
    }

    void log(String message, LogLevel level) {
    }

    def httpRequest(Map map) {
    }

    def libraryResource(String str) {
    }

    def withRetry(Closure body) {
    }
}
