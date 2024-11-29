public class GAV {

    private String groupId
    private String artifactId
    private String version

    public GAV(String groupId, String artifactId, String version) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    String getGroupId() {
        return groupId
    }

    String getArtifactId() {
        return artifactId
    }

    String getVersion() {
        return version
    }

}
