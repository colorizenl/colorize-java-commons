Colorize Java Commons
=====================

[![Maven Central](https://img.shields.io/maven-central/v/nl.colorize/colorize-java-commons)](
https://central.sonatype.com/artifact/nl.colorize/colorize-java-commons)
[![Documentation](https://img.shields.io/badge/docs-javadoc-yellow)](
https://api.clrz.nl/colorize-java-commons)
[![License](https://img.shields.io/badge/license-apache_2.0-purple)](
https://www.apache.org/licenses/LICENSE-2.0)

Set of Java libraries for web, desktop, and mobile applications. It is used in all Java-based
applications and libraries developed by [Colorize](http://www.colorize.nl), where it has been
in use since 2007.

The library focuses on portability, and supports a wide variety of platforms and environments.
It can be used in back-end applications, in cloud environments, in desktop applications
on  Windows/Mac/Linux, in mobile apps on Android and iOS, and in the browser
via [TeaVM](http://teavm.org)).

The reason for this large variety of environments is that the library started out as a foundation 
for cross-platform desktop applications. It was later adopted for back-end applications, and
now supports both scenarios. See the [portability](#portability) section for more information
on using this library across different platforms.

Usage
-----

The library is available from the Maven Central repository. To use it in a Maven project, add it 
to the dependencies section in `pom.xml`:

```xml
<dependency>
    <groupId>nl.colorize</groupId>
    <artifactId>colorize-java-commons</artifactId>
    <version>2026.1</version>
</dependency>
```
    
The library can also be used in Gradle projects:

```groovy
dependencies {
    implementation "nl.colorize:colorize-java-commons:2026.1"
}
```
    
Documentation
-------------

- [JavaDoc](http://api.clrz.nl/colorize-java-commons/)

Portability
-----------

This library was created with portability in mind, as explained in the introduction. However, that
does not mean that every single class is available on every single platform. The package structure
is used to indicate which classes are supported on which platforms:

| Package                      | Back-end? | Desktop? | Mobile? | Browser/TeaVM? |
|------------------------------|-----------|----------|---------|----------------|
| `nl.colorize.util`           | ✅         | ✅        | ✅       | ✅              |
| `nl.colorize.util.animation` | ✅         | ✅        | ✅       | ✅              |
| `nl.colorize.util.cli`       | ✅         | ✅        | ❌       | ❌              |
| `nl.colorize.util.http`      | ✅         | ✅        | ❌       | ❌              |
| `nl.colorize.util.swing`     | ❌         | ✅        | ❌       | ❌              |              

In the table above, "back-end" refers to any type of headless back-end application or service. 
Deployment can range from [Docker](https://www.docker.com) containers to cloud-native 
environments like [Google App Engine](https://cloud.google.com/appengine?hl=en).

Examples
--------

Refer to the [documentation](#documentation) for a full overview of available utility classes.
This section shows a few code examples for commonly used utilities.

### Working with asynchronous events in client-side applications

In client-side applications (desktop, mobile, browser), you can't just block the user interface
thread while waiting for an expensive operation or network call to finish. Such long-running
tasks are typically performed in the background. These background tasks then communicate events
back to the user interface thread, so that the application can display the results. If the
background task fails, the error *also* needs to be communicated back to the user interface
thread.

The library contains a number of small "primitives" to facilitate this type of workflow. 
All of these primitives can be used cross-platform, including in the browser via TeaVM.

```java
// Allows for a publisher/subscriber workflow. Asynchronous events
// (and errors) are published to subscribers.
Subject<Response> task = Subject.runAsync(() -> externalService.someCall());
task.subscribe(this::processResponse);
task.subsctibeErrors(this::showError);

// Wraps a property so that subscribers can be notified whenever
// the value changes.
Signal<String> name = Signal.of("john");
name.getChanges().subscribe(this::updateUI);
name.set("jim");

// Wraps a collection so that subscribers can be notified when
// elements are added or removed.
SubscribableCollection<String> names = SubscribableCollection.wrap(new ArrayList<>());
names.getAddedElements().subscribe(this::updateUI);

// Caches a single value so that the operation is only performed
// once, and subsequent calls return the cached value. Uses lazy
// evaluation.
Memoized<Result> result = Memoized.compute(this::expensiveOperation);

// Caches multiple values based on a key, so they are only
// calculated once. Also uses lazy evaluation.
Cache<String, Result> responseCache = Cache.from(key -> expensiveOperation(key));
```

### Swing component library

Refer to `CustomComponentsUIT` for an example application. This example uses the following
components:

- `Accordion`: Consists of different sections which can be expanded or collapsed.
- `CircularLoader`: A "spinner" that indicates the application is loading.
- `FormPanel`: A form builder that makes it easy to add name/field pairs with a common form layout.
- `ImageViewer`: Easily display an image in a Swing component with pan and zoom controls.
- `MultiLabel`: An extension of `JLabel` that automatically word-wraps long lines.
- `Popups`: Shows various pop-up and modal dialog windows.
- `PropertyEditor`: UI for adding, removing, or changing name/value properties.
- `SimpleTable`: A simplified version of `JTable` that is easier to work with.
- `SuggestingComboBox`: Combination of a text field and a `JComboBox` that matches suggestions. 

In addition to the cross-platform component library, `MacIntegrationUIT` contains an example
application that shows integration with some Mac system functionality.

### Animation framework

Refer to `InterpolationUIT` for an interactive example. The following example creates a timeline
that moves the ball across the screen in 0.4 seconds, using easing animation interpolation:

```java
Timeline anim = new Timeline(Interpolation.EASE)
    .addKeyFrame(0f, 0f);
    .addKeyFrame(0.4f, 800f);
```

The timeline can then be used for animation. The following example shows how to animate a Swing
component's background color from red to blue:

```java
SwingAnimator animator = new SwingAnimator();
animator.start();

JPanel target = new JPanel();
target.setOpaque(true);
target.setBackground(Color.RED);
target.setPreferredSize(new Dimension(200, 200));

JButton animateButton = new JButton("Animate background color");
animateButton.addActionListener(e -> animator.animateBackgroundColor(target, Color.BLUE, 1f));
```

### Image manipulation

The library includes `Utils2D` for working with and manipulating images. The sample application
`ImageManipulationUIT` includes an interactive showcase for the various image effects.

### Command line interface

The `CommandLineArgumentParser` can be used to define and parse arguments for command line
applications. It is quite similar to the annotation-based approach used by
[Args4j](https://github.com/kohsuke/args4j), which is excellent but has not been updated
since 2016.

The following example shows how to define a simple command line interface:

```java
public class MyApp {
    @Arg(name = "--input", usage = "Input directory")
    public File inputDir

    @Arg
    public boolean overwrite;

    public static void main(String[] argv) {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser(MyApp.class);
        MyApp app = argParser.parse(argv, MyApp.class);
    }
}
```

Build instructions
------------------

Building the library requires the following:

- [Java JDK](http://java.oracle.com) 25+
- [Gradle](http://gradle.org)

The following Gradle build tasks are available:

- `gradle clean` cleans the build directory.
- `gradle assemble` creates the JAR file for distribution.
- `gradle test` runs all unit tests.
- `gradle coverage` runs all unit tests and reports on test coverage.
- `gradle javadoc` generates the JavaDoc API documentation.
- `gradle dependencyUpdates` checks for and reports on library updates.
- `gradle publishToMavenCentral` publishes the library to Maven Central.
  Requires [credentials](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets).

License
-------

Copyright 2007-2026 Colorize

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
