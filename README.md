# Jenkins Shared library unit tests example

This is an example that shows how you can organize the architecture of Jenkins Shared Library to use Jenkins-steps inside classes.
It also shows how you can cover the logic with unit tests using the Spock framework with this approach.

The library itself provides classes for communicating with Jenkins, Bitbucket via REST API, a wrapper for git, and other things.

# Build and run tests

Run `gradle test` in `jenkins-shared-library` directory

Tested on:
* gradle 8.10.2;
* jdk 21.0.7
