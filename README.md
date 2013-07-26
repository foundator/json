json
====

A simple and fast JSON library for Scala.

There are a ton of JSON libraries for Scala out there, but I was fustrated with their complexity and magic. "What on earth is so complicated about JSON?" I thought. Well, the answer is nothing. Hence this library.


Data Structure
==============

The data structure is modelled closely after the json.org specification (RFC 4627), and represents all valid JSON values. There's no magic:

```scala
sealed abstract class Json
case class  JsonObject  ( members  : (String, Json)* ) extends Json
case class  JsonArray   ( elements : Json*           ) extends Json
case class  JsonString  ( text     : String          ) extends Json
case class  JsonNumber  ( number   : Double          ) extends Json
case class  JsonBoolean ( value    : Boolean         ) extends Json
case object JsonNull                                   extends Json
```

Note that the order of the members in the JsonObject is preserved, though this is not required by the specification.


Usage
=====

In order to build up JSON values, simply use the above constructors, eg:

```scala
val json = JsonObject(
    "x" -> JsonNumber(0),
    "y" -> JsonNumber(1)
)
```

You can of course also use pattern matching to extract values from a JSON data structure.


Serialization
-------------

You can read and write JSON data structures via the `Json` object:


```scala
val compact = Json.write(json, None)
val pretty = Json.write(json, Some("    "))
Json.write(new File("myfile.json"), json, None)
```

* The first example returns the JSON as a string with no indentation or line breaks. 
* The second example is similar, but pretty prints the JSON with the given 4 spaces of indentation.
* The third example writes to a file instead. You can also write to a stream (UTF-8) or a writer (any encoding).

Deserialization
---------------

```scala
val a = Json.read(compact) // or pretty
val b = Json.read(new File("myfile.json"))
```

* The first example reads the JSON data structure from the provided string. 
* The second example reads from a file instead. You can also read from a stream (will detect UTF-8 and both endian variants of UTF-16 and UTF-32), or a reader.
