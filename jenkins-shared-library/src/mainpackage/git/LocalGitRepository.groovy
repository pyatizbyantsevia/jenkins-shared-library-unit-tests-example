package mainpackage.git

public class LocalGitRepository {

    private String sshBitBucketURL
    private String sshCred
    private String localRepoDirectory
    private Script steps

    public LocalGitRepository(String sshBitBucketURL, String sshCred, String localRepoDirectory, Script steps) {
        this.sshBitBucketURL = sshBitBucketURL
        this.sshCred = sshCred
        this.localRepoDirectory = localRepoDirectory
        this.steps = steps
    }

    public void initWithNewBranch(String newBranch) {
        steps.dir(this.localRepoDirectory) {
            steps.sh 'git init'
            steps.sh "git checkout -b ${newBranch}"
            steps.sh "git remote add origin ${this.sshBitBucketURL}"
        }
    }

    public void cloneWithSpecificBranch(String branchToClone) {
        steps.dir(this.localRepoDirectory) {
            steps.git(credentialsId: this.sshCred, url: this.sshBitBucketURL, branch: branchToClone)
        }
    }

    public void commit(ArrayList<String> filesToCommit, String commitMessage) {
        steps.dir(this.localRepoDirectory) {
            steps.sh("git add ${filesToCommit.join(' ')}")
            steps.sh("""git commit -m \"${commitMessage}\"""")
        }
    }

    public void push(String branchToPush) {
        steps.dir(this.localRepoDirectory) {
            steps.sshagent([this.sshCred]) {
                steps.sh "git push --set-upstream origin ${branchToPush}"
            }
        }
    }

}

