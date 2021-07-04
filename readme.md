Colorize Java Commons
=====================

Java library containing utility classes for web, desktop, and mobile applications. It is used by 
all Java-based applications and libraries developed by [Colorize](http://www.colorize.nl/en/). 
The library provides the following features:

- an animation framework that includes scheduling and keyframe animation
- a component library for Swing applications
- a command line argument parser
- assists in sending HTTP requests from applications
- reading and writing CSV
- automatic application logging configuration
- various other utility classes

The library focuses on portability, and supports a wide variety of platforms and environments:

- Windows (desktop, server)
- Mac OS (desktop)
- Linux (desktop, server)
- Google Cloud (cloud)
- AWS (cloud)
- Android
- iOS (via [RoboVM](http://robovm.mobidevelop.com))
- Browser (via [TeaVM](http://teavm.org))

Usage
-----

The library is available from the Maven Central repository. To use it in a Maven project, add it 
to the dependencies section in `pom.xml`:

    <dependency>
        <groupId>nl.colorize</groupId>
        <artifactId>colorize-java-commons</artifactId>
        <version>2021.8</version>
    </dependency>  
    
The library can also be used in Gradle projects:

    dependencies {
        compile "nl.colorize:colorize-java-commons:2021.8"
    }
    
Documentation
-------------

- [JavaDoc](http://api.clrz.nl/colorize-java-commons/)

Usage examples
--------------

### Command line interface

The `CommandLineArgumentParser` can be used to define and parse arguments for command line
applications. It uses an approach that is quite different from the annotation-based approach
used by [Args4j](https://github.com/kohsuke/args4j), which is excellent but has not been updated
since 2016. Instead, it is quite similar to Python's `argparse` module.

The following example shows how to define a simple command line interface:

```
public static void main(String[] args) {
    CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp");
    argParser.add("--input", "Input directory");
    argParser.addOptional("--overwrite", false, "Overwrites existing values");
    argParser.parseArgs(args)
 
    File inputDir = argParser.getFile("input");
    boolean overwrite = argParser.getBool("overwrite");
}
```

### Swing component library

Refer to `CustomComponentsUIT` for an example application. `MacIntegrationUIT` contains an example
application that shows integration with some Mac system functionality.

### Animation framework

Refer to `InterpolationUIT` for an interactive example.

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
- `gradle -b build-mavencentral.gradle publish` publishes to Maven central (requires account) 

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
