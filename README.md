foundator-json
==============

A simple and fast JSON library for Scala.

There are [a ton of JSON libraries for Scala](#other-json-libraries) out there, but I was fustrated with their complexity and magic. "What on earth is so complicated about JSON?" I thought. Well, the answer is nothing. Hence this library.


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
val j = JsonObject(
    "address" -> JsonObject("city" -> JsonString("Copenhagen")),
    "luckyNumbers" -> JsonArray(JsonNumber(7), JsonNumber(13))
)
```

For queries, you can either use plain old pattern matching on the `Json` data structure, or you can use the following query methods:

| Invocation | Target | Result | Description |
|------------|--------|--------|-------------|
| `j("city", "address")` | `JsonObject` | `Option[Json]` | Accesses "address" of the "city" field of `j`. |
| `j(0)` | `JsonArray` | `Option[Json]` | Accesses the first element of `j`. |
| `j.members` | `JsonObject` | `Option[Map[String, Json]]` | Returns a map with all the members of `j`. |
| `j.elements` | `JsonArray` | `Option[List[Json]]` | Returns a list with all the elements of `j`. |
| `j.string` | `JsonString` | `Option[String]` | Returns the string inside `j`. |
| `j.number` | `JsonNumber` | `Option[Double]` | Returns the double inside `j`. |
| `j.boolean` | `JsonBoolean` | `Option[Boolean]` | Returns the boolean inside `j`. |
| `j.isNull` | (any JSON) | `Boolean` | Returns true if `j == JsonNull`. |

The above methods return `None` if `j` is not an instance of the target type, or if the accessed element or member doesn't exist. Otherwise they return `Some(v)` where `v` is the value. The exception is `isNull`, which always returns a plain boolean.

Since the `Option` type is a monad, you can also use the `for ... yield ...` syntax for querying:

```scala
val Some(city) = j("address", "city").string
val Some(lucky) = for(ns <- j("luckyNumbers"); n <- ns(1); d <- n.number) yield d
```

After running the above, `city == "Copenhagen"` and `lucky == 13`.


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


Other JSON libraries
====================

And why I felt a new one was required.

* Scala's standard JSON type: Uses `Any` to represent the JSON types `Number`, `String`, `Boolean` and `Null`, defeating type safety.
* The Play JSON library: Has `JsUndefined` ... but there's no such thing in JSON! Also, is not a self-contained library - you have to depende on the whole Play framework.
* Argonaut: Has data structure much like the foundator-json library - unfortunately, it's off limits for library users, since it's private.
* Jerkson: Has a data structure where `JField` can occur anywhere, even outside objects.
* And many, many more.

Many of these also expose a large and complex API, which is very strange given the triviality of the core problem.


What happens if this project is abandoned?
------------------------------------------

Nothing. This library already works and is feature complete. Its source and its binary are both hosted by 3rd parties that are very unlikely to disappear.

Don't get me wrong. There are plenty of other functionality one might want when dealing with JSON, such as automatic conversion from and to user defined types and streaming. However, these do not belong in the core JSON library - they should be in a separate library.
