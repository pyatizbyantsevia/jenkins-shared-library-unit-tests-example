package mainpackage.api

abstract class API {
    protected String url
    protected String credential

    API(Map args) {
        if (!args.url || !args.credential) {
            throw new IllegalArgumentException("When creating an API object, you must pass the url and credential parameters")
        }
        this.url = args.url
        this.credential = args.credential
    }
}
