#	Building HA-JDBC

To build from source, first obtain the source code from either:

*	[Source code archive][tags]
*	[Version control](source-repository.html)

##	Build requirements

*	[JDK 1.8+][jdk]
*	[Maven 3.3+][maven]

##	Building the JAR

	mvn package

The generated jar file resides in the `target` directory.
HA-JDBC has no required dependencies.
See the [Dependencies](dependencies.html) documentation to download any optional dependencies.

##	Building the project site/documentation

	mvn site:site site:stage

The generated documentation resides in the `target/staging` directory.

[tags]: http://github.io/ha-jdbc/ha-jdbc/tags "HA-JDBC source code archive"
[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html "Java SE"
[maven]: http://maven.apache.org/download.html "Apache Maven Project"
