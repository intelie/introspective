# Introspective

JVM Introspection Utilities

Largely based on https://github.com/apangin/helfy.

The main ability this lib has right now is to inspect the total amount of allocated bytes 
for any thread. But you can inspect pretty much any internal JVM object with this tool.

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
    <version>0.1</version>
</dependency>
```

Then, you can use it like that:

```java
ThreadResources.allocatedBytes(Thread.currentThread())
```
