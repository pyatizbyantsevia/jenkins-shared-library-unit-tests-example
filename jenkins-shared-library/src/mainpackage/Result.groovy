package mainpackage

enum Result {
    SUCCESS,
    UNSTABLE,
    NOT_BUILT,
    ABORTED,
    FAILURE

    static Result valueOfResult(String result) {
        def found = values().find { it.name() == result }
        if (!found) {
            throw new IllegalArgumentException("No enum constant mainpackage.Result.$result")
        }
        return found
    }
}