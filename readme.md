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

- desktop (Windows, Mac OS, Linux)
- Android
- iOS (via [RoboVM](http://robovm.mobidevelop.com))
- Google Cloud
- browsers (via [TeaVM](http://teavm.org))

Usage
-----

The library is available from the Maven Central repository. To use it in a Maven project, add it 
to the dependencies section in `pom.xml`:

    <dependency>
        <groupId>nl.colorize</groupId>
        <artifactId>colorize-java-commons</artifactId>
        <version>2019.2</version>
    </dependency>  
    
The library can also be used in Gradle projects:

    dependencies {
        compile "nl.colorize:colorize-java-commons:2019.2"
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

Copyright 2007-2019 Colorize

The source code is licensed under the Apache License 2.0, meaning you can use it free of charge 
in commercial and non-commercial projects as long as you mention the original copyright.
The full license text can be found at 
[http://www.colorize.nl/code_license.txt](http://www.colorize.nl/code_license.txt).

By using the source code you agree to the Colorize terms and conditions, which are available 
from the Colorize website at [http://www.colorize.nl/en/](http://www.colorize.nl/en/).
