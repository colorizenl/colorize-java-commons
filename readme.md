Colorize Java Commons
=====================

Library containing utility classes for web, desktop, and mobile applications. It is used by all
Java-based applications and libraries developed by [Colorize](http://www.colorize.nl/en/). The 
library includes the following features:

- assists in creating and sending HTTP requests
- an animation framework that includes scheduling and keyframe animation
- a lightweight REST API framework that can be used in environments with limited resources
- a framework for managing, selecting, filtering, sorting, and performing calculations on data sets
- a component library for Swing applications
- automatic logger configuration
- working with Apple's Property List file format
- various other utility classes

The library focuses on portability, and supports a wide variety of platforms and environments:

- Windows (desktop, server)
- Mac OS (desktop)
- Linux (desktop, server)
- Google Cloud (cloud)
- AWS (cloud)
- Android
- iOS via [RoboVM](http://robovm.mobidevelop.com)
- Browser via [TeaVM](http://teavm.org)

Usage
-----

The library is available from the Maven Central repository. To use it in a Maven project, add it 
to the dependencies section in `pom.xml`:

    <dependency>
        <groupId>nl.colorize</groupId>
        <artifactId>colorize-java-commons</artifactId>
        <version>2020.1.10</version>
    </dependency>  
    
The library can also be used in Gradle projects:

    dependencies {
        compile "nl.colorize:colorize-java-commons:2020.1.10"
    }

Build instructions
------------------

The build is cross-platform and supports Windows, macOS, and Linux, but requires the following 
software to be available:

- [Java JDK](http://java.oracle.com) 11+
- [Gradle](http://gradle.org)

The following Gradle build tasks are available:

- `gradle clean` cleans the build directory
- `gradle assemble` creates the JAR file for distribution
- `gradle test` runs all unit tests
- `gradle coverage` runs all unit tests and reports on test coverage
- `gradle javadoc` generates the JavaDoc API documentation

License
-------

Copyright 2007-2020 Colorize

The source code is licensed under the Apache License. Refer to
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0) for
the full license text.
