json
====

A simple and fast JSON library for Scala.


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
val json = JsonObject (
    "x" -> JsonNumber(0),
    "y" -> JsonNumber(1)
)
```

You can of course also use pattern matching to extract values from a JSON data structure.


Serialization
-------------

You can read and write JSON data structures via the `Json` object:


```scala
val compact = Json.write(json, None) // Write to a string with no indentation or line breaks.
val pretty = Json.write(json, Some("    ")) // Write to a string with 4 spaces of indentation and line breaks.
Json.write(new File("myfile.json"), json, None) // You can also write directly to a file, stream or writer.
```
