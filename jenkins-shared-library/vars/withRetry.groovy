import mainpackage.LogLevel
import mainpackage.exception.RetryException

def call(int maxRetries = 5, int delay = 60, Closure body) {
    retry(maxRetries) {
        try {
            return body.call()
        } catch (Exception ex) {
            log(ex.getMessage(), LogLevel.WARNING)
            sleep delay
            throw new RetryException("Retry")
        }
    }
}