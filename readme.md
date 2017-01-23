Colorize Java Commons
=====================

Core library that is used in all of Colorize's Java-based applications. The library includes
the following features:

- assists in creating and sending HTTP requests
- an animation framework that includes scheduling and keyframe animation
- a REST API framework that can be used in both servlets and other types of web app environments
- automatic logger configuration
- a component library for Swing applications
- working with Apple's Property List file format
- various other utility classes

The library is used in a large variety of application types: web applications, the cloud (Google
App Engine), desktop applications (Windows/macOS/Linux), mobile applications (Android), command
line tools, REST APIs, and other libraries. In order to support these different environments the 
source code has to be extremely portable. 

Building
--------

Building the library requires the [Java JDK](http://java.oracle.com) and
[Gradle](http://gradle.org). 

The following Gradle build tasks are available:

- `gradle clean` cleans the build directory
- `gradle assemble` creates the JAR file for distribution
- `gradle test` runs all unit tests, then reports on test results and test coverage
- `gradle javadoc` generates the JavaDoc API documentation

License
-------

Copyright 2009-2017 Colorize

The source code is licensed under the Apache License 2.0, meaning you can use it free of charge 
in commercial and non-commercial projects as long as you mention the original copyright.
The full license text can be found at 
[http://www.colorize.nl/code_license.txt](http://www.colorize.nl/code_license.txt).

By using the source code you agree to the Colorize terms and conditions, which are available 
from the Colorize website at [http://www.colorize.nl/en/](http://www.colorize.nl/en/).
