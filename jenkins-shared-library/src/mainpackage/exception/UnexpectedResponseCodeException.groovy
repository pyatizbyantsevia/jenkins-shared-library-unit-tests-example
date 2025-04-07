package mainpackage.exception

class UnexpectedResponseCodeException extends Exception {
    Integer unexpectedResponseCode

    UnexpectedResponseCodeException(String message, Integer unexpectedResponseCode) {
        super(message)
        this.unexpectedResponseCode = unexpectedResponseCode
    }
}
