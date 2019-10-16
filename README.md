# nem2-sdk for Java/Kotlin/Scala
#

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.nem/sdk/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.nem/sdk)
[![Build Status](https://api.travis-ci.org/nemtech/nem2-sdk-java.svg?branch=master)](https://travis-ci.org/nemtech/nem2-sdk-java)
[![Coverage Status](https://coveralls.io/repos/github/nemtech/nem2-sdk-java/badge.svg?branch=master)](https://coveralls.io/github/nemtech/nem2-sdk-java?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The official nem2-sdk for Java, Kotlin and Scala to work with the NEM2 (a.k.a Catapult).

## Requirements

- Java 8
- Java 9 has not been tested yet

## Installation

### Open API Generated Libraries (Optional)

The SDK libs depend on Open API 3 generated clients. The clients jars are automatically generated and deployed into Maven central. 
The following steps are required if you are working on the Open API specification, you want to change or tune the generator. 

If you want to build the clients,  execute the following to generate and install the libraries.

```
./gradlew -b ./openapi-generator/build.gradle clean generate
./gradlew -b ./openapi-generator/build.gradle install
```

## Usage

Each SDK user can depend on the best library for its need (example, ``sdk-vertx-client`` for server developers or ``sdk-okhttp-client`` for android developers).

### Maven

```xml
<dependency>
    <groupId>io.nem</groupId>
    <artifactId>sdk-vertx-client</artifactId>
    <version>0.14.0</version>
</dependency>
```

OR

```xml
<dependency>
    <groupId>io.nem</groupId>
    <artifactId>sdk-okhttp-client</artifactId>
    <version>0.14.0</version>
</dependency>
```


### Gradle

```compile 'io.nem:sdk-vertx-client:0.14.0```

OR

```compile 'io.nem:sdk-okhttp-client:0.14.0```


### SBT

```libraryDependencies += "io.nem" % "sdk-vertx-client" % "0.14.0"```

OR

```libraryDependencies += "io.nem" % "sdk-okhttp-client" % "0.14.0"```


## Documentation and Getting Started

Get started and learn more about nem2-sdk-java, check the [official documentation][docs].

Check SDK reference [here][sdk-ref]

## Modules

The SDK is composed of multiple sub-modules/folders:

- **openapi-generator:** Utility folder that knows how to generate, install and release to maven central the different API clients from the open api 3 specification. The generated clients are `api-vertx-client`, `api-okhttp-gson-client`, `api-jersey2-client`. We may add more if customers require them.
- **sdk-core:** This module includes the model objects, interfaces and common utility classes. It is Vertx, ok-http, gson, etc agnostic. Clients won't depend on this jar directly, they will depend on one of the implementations below.
- **sdk-vertx-client:** The nem2-sdk-java Implementation that uses Vertx and generated `api-vertx-client` lib and dtos. A client may depend on this SDK implementation if Vertx is the selected implementation (e.g. server users).
- **sdk-okhttp-client:** The nem2-sdk-java Implementation that uses OkHttp and the generated `api-okhttp-gson-client`. A client may depend on this SDK implementation if OkHttp is the selected implementation (e.g. android users).
- **integration-tests:** This module is in charge of running integration tests against all implementations. The integration tests exercise how the implementation work against a given catapult server.


## nem2-sdk Releases

The release notes for the nem2-sdk can be found [here](CHANGELOG.md).

## Contributing

This project is developed and maintained by NEM Foundation. Contributions are welcome and appreciated. You can find [nem2-sdk on GitHub][self];
Feel free to start an issue or create a pull request. Check [CONTRIBUTING](CONTRIBUTING.md) before start.

## Getting help

We use GitHub issues for tracking bugs and have limited bandwidth to address them.
Please, use the following available resources to get help:

- [nem2-cli documentation][docs]
- If you found a bug, [open a new issue][issues]

## License

Copyright (c) 2018 NEM
Licensed under the [Apache License 2.0](LICENSE)

[self]: https://github.com/nemtech/nem2-sdk-java
[docs]: http://nemtech.github.io/getting-started/setup-workstation.html
[issues]: https://github.com/nemtech/nem2-sdk-java/issues
[sdk-ref]: http://nemtech.github.io/nem2-sdk-java/javadoc/0.14.0/
