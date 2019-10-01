# Introspective

JVM Introspection Utilities

Initially based on https://github.com/apangin/helfy.

This library has two main features:

- Inspect the total amount of allocated bytes for any thread, with an implementation orders of magnitude faster than ThreadMXBean;
- Estimate recursively the total amount of bytes an object holds.

Also, with the tools provided here, you can inspect pretty much any internal JVM object with this tool.

We tested it on Linux, in OpenJDK versions 8 to 13, but it should work well in other systems.
No guarantee, though.

We had to use introspection because ThreadMXBean is just too slow for our needs.

## Usage

Introspective is available through Maven Central repository, just add the following
dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>net.intelie.introspective</groupId>
    <artifactId>introspective</artifactId>
    <version>0.2</version>
</dependency>
```

Then, you can use it like that:

```java
ThreadResources.allocatedBytes(Thread.currentThread())
```
