Colorize Java Commons
=====================

Library containing utility classes for web, desktop, and mobile applications. It is used by all
Java-based applications and libraries developed by [Colorize](http://www.colorize.nl/en/). The 
library includes the following features:

- assists in creating and sending HTTP requests
- an animation framework that includes scheduling and keyframe animation
- a REST API framework that can be used in both servlets and other types of web app environments
- a framework for managing, selecting, filtering, sorting, and performing calculations on data sets
- a component library for Swing applications
- automatic logger configuration
- working with Apple's Property List file format
- various other utility classes

The library is used in a large variety of application types: web applications, the cloud (Google
App Engine), desktop applications (Windows/macOS/Linux), mobile applications (Android), command
line tools, REST APIs, and other libraries. In order to support these different environments the 
source code has to be extremely portable. 

Build
-----

The build is cross-platform and supports Windows, macOS, and Linux, but requires the following 
software to be available:

  - [Java JDK](http://java.oracle.com)
  - [Gradle](http://gradle.org)

The following Gradle build tasks are available:

  - `gradle clean` cleans the build directory
  - `gradle assemble` creates the JAR file for distribution
  - `gradle test` runs all unit tests, then reports on test results and test coverage
  - `gradle javadoc` generates the JavaDoc API documentation

License
-------

Copyright 2007-2017 Colorize

The source code is licensed under the Apache License 2.0, meaning you can use it free of charge 
in commercial and non-commercial projects as long as you mention the original copyright.
The full license text can be found at 
[http://www.colorize.nl/code_license.txt](http://www.colorize.nl/code_license.txt).

By using the source code you agree to the Colorize terms and conditions, which are available 
from the Colorize website at [http://www.colorize.nl/en/](http://www.colorize.nl/en/).
