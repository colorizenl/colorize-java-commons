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
        <version>2021.1.1</version>
    </dependency>  
    
The library can also be used in Gradle projects:

    dependencies {
        compile "nl.colorize:colorize-java-commons:2021.1.1"
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

Copyright 2007-2021 Colorize

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
