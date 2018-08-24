# pzsvc-preview-generator
This is an example of a user service which generates thumbnail previews of images.

***
## Requirements
Before building and/or running the pz-search-query service, please ensure that the following components are available and/or installed, as necessary:
- [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (JDK for building/developing, otherwise JRE is fine)
- [ElasticSearch](https://www.elastic.co/)
- [Eclipse](https://www.eclipse.org/downloads/), or any maven-supported IDE
- [Amazon S3](https://docs.aws.amazon.com/AmazonS3/latest/gsg/GetStartedWithS3.html) bucket access
- Access to Nexus is required to build

Ensure that the nexus url environment variable `ARTIFACT_STORAGE_URL` is set:

	$ export ARTIFACT_STORAGE_URL={Artifact Storage URL}

For additional details on prerequisites, please refer to the Piazza Developer's Guide [Core Overview](https://github.com/venicegeo/pz-docs/blob/master/documents/devguide/02-pz-core.md) or the 
[prerequisites for using Piazza](https://github.com/venicegeo/pz-docs/blob/master/documents/devguide/03-jobs.md) section for additional details.

***
## Setup, Configuring, & Running
### Setup

Create the directory the repository must live in, and clone the git repository:

    $ mkdir -p {PROJECT_DIR}/src/github.com/venicegeo
	$ cd {PROJECT_DIR}/src/github.com/venicegeo
    $ git clone git@github.com:venicegeo/pzsvc-preview-generator.git
    $ cd pzsvc-preview-generator

>__Note:__ In the above commands, replace {PROJECT_DIR} with the local directory path for where the project source is to be installed.

### Configuring
As noted in the Requirements section, to build and run this project, ElasticSearch is required. The `application.properties` file controls URL information for ElasticSearch connection configurations.

To edit the port that the service is running on, edit the `server.port` property.

### Running
To build and run the preview generator locally, pzsvc-preview-generator can be run using Eclipse any maven-supported IDE. Alternatively, pzsvc-preview-generator can be run through command line interface (CLI), by navigating to the project directory and run:

`mvn clean install -U spring-boot:run`

This will run a Tomcat server locally with the Gateway service running on port 8086 (unless port was modified per 'Configuring' section).

> __Note:__ This Maven build depends on having access to the `Piazza-Group` repository as defined in the `pom.xml` file. If your Maven configuration does not specify credentials to this Repository, this Maven build will fail.

### Running Unit Tests

To run the ServiceController unit tests from the main directory, run the following command:

	$ mvn test

