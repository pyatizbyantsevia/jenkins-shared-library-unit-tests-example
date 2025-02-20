package mainpackage.mock

import mainpackage.LogLevel

class MockSteps extends Script {

    @Override
    Object run() {
        return null
    }

    void log(String message, LogLevel level) {
    }

    void echo(String message) {
    }

    def httpRequest(Map map) {
    }

    def libraryResource(String path) {
    }
}