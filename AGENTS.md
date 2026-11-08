# AGENTS.md

This file provides instructions to agents working on this repository.

## Project summary

This is a Java library that is used for applications running in the cloud, desktop applications,
mobile apps, and in the browser via [TeaVM](https://teavm.org).

## Commands

- `gradle test` runs all unit tests.
- `gradle test --tests '*SomethingTest'` runs unit tests in a specific class.
- `gradle spotbugsMain` runs the SpotBugs quality checks.
- `gradle assemble` does a full build.

## Architecture

- Everything in `nl.colorize.util.swing` is allowed to depend on AWT and Swing. All other packages
  should *not* depend on AWT or Swing.

## Code style

- Use the UTF-8 character encoding, 4 space indent, Unit line breaks.
- In general, code style is not very strict and not enforced. Try to follow a reasonable style.
- Maximum line length is ideally 100 characters, but this can be ignored if necessary.
- Use Lombok annotations to reduce boilerplate code.
