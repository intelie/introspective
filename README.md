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
    <version>0.3</version>
</dependency>
```

### Inspecting total allocated bytes

```java
ThreadResources.allocatedBytes(Thread.currentThread())
```

### Estimating object size recursively

```java
Map<Object, Object> test = new HashMap<>();
test.put(111, Arrays.asList("aaa", 222));
test.put(333.0, Collections.singletonMap("bbb", 444));

ObjectSizer sizer = new ObjectSizer();
sizer.resetTo(test);

while (sizer.moveNext()) {
    System.out.println(sizer.bytes() + " bytes: " + sizer.path());
    System.out.println("  class: " + sizer.type());
    System.out.println("  value: " + sizer.current());
}
```

The output for the snippet above will be:

```
48 bytes: 
  class: class java.util.HashMap
  value: {333.0={bbb=444}, 111=[aaa, 222]}
80 bytes: .table
  class: class [Ljava.util.HashMap$Node;
  value: [Ljava.util.HashMap$Node;@e6ea0c6
32 bytes: .table[4]
  class: class java.util.HashMap$Node
  value: 333.0={bbb=444}
24 bytes: .table[4].key
  class: class java.lang.Double
  value: 333.0
40 bytes: .table[4].value
  class: class java.util.Collections$SingletonMap
  value: {bbb=444}
48 bytes: .table[4].value.k
  class: class java.lang.String
  value: bbb
16 bytes: .table[4].value.v
  class: class java.lang.Integer
  value: 444
16 bytes: .table[4].value.entrySet
  class: class java.util.Collections$SingletonSet
  value: [bbb=444]
24 bytes: .table[4].value.entrySet.element
  class: class java.util.AbstractMap$SimpleImmutableEntry
  value: bbb=444
32 bytes: .table[15]
  class: class java.util.HashMap$Node
  value: 111=[aaa, 222]
16 bytes: .table[15].key
  class: class java.lang.Integer
  value: 111
24 bytes: .table[15].value
  class: class java.util.Arrays$ArrayList
  value: [aaa, 222]
24 bytes: .table[15].value.a
  class: class [Ljava.io.Serializable;
  value: [Ljava.io.Serializable;@57fa26b7
48 bytes: .table[15].value.a[0]
  class: class java.lang.String
  value: aaa
16 bytes: .table[15].value.a[1]
  class: class java.lang.Integer
  value: 222
16 bytes: .entrySet
  class: class java.util.HashMap$EntrySet
  value: [333.0={bbb=444}, 111=[aaa, 222]]
```