package mainpackage.polling

import java.util.concurrent.TimeoutException

class Poller {
    static def poll(Closure resultSupplier,
                    Closure actionSupplier = null,
                    int intervalSeconds = 5,
                    int timeoutSeconds = 60) {
        def intervalMillis = intervalSeconds * 1000
        def timeoutMillis = timeoutSeconds * 1000

        def startTime = System.currentTimeMillis()
        while (true) {
            def result = resultSupplier.call()

            if (result) {
                return result
            }

            if (actionSupplier) {
                actionSupplier.call()
            }

            Thread.sleep(intervalMillis)

            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new TimeoutException("Polling timed out after ${timeoutSeconds} seconds.")
            }
        }
    }
}
