void cleanUpDir(String dirToCleanUp) {
    dir(dirToCleanUp) {
        sh("rm -rf ./*")
    }
}

return this