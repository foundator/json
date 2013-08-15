package org.foundator.json

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.io.{File, ByteArrayInputStream, OutputStreamWriter, ByteArrayOutputStream}
import java.nio.charset.Charset

@RunWith(classOf[JUnitRunner])
class TestJson extends FunSuite {

    val json0 = JsonNumber(0)
    val json1 = JsonNull
    val json2 = JsonBoolean(true)
    val json3 = JsonNumber(-Math.PI / 10)
    val json4 = JsonString("Hello \r\n\t\u0000\u0001\"\\ World! Æø")
    val json5 = JsonArray()
    val json6 = JsonArray(json3)
    val json7 = JsonArray(json3, json4)
    val json8 = JsonObject()
    val json9 = JsonObject("foo" -> json3)
    val json10 = JsonObject("a" -> json3, "b" -> json4)
    val json11 = JsonObject("x" -> json7, "y" -> json10)
    val json12 = JsonArray(json11)
    val jsons = List(json0, json1, json2, json3, json4, json5, json6, json7, json8, json9, json10, json11, json12)

    test("Json.read(Json.write(json, None)) == json") {
        for(json <- jsons) {
            assert(Json.read(Json.write(json, None)) === json)
        }
    }

    test("UTF-8/16/32 encodings should be auto-detected") {
        for(charset <- List("UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE")) {
            for(json <- jsons) {
                val out = new ByteArrayOutputStream()
                Json.write(new OutputStreamWriter(out, Charset.forName(charset)), json, None)
                assert(Json.read(new ByteArrayInputStream(out.toByteArray)) === json)
            }
        }
    }

    test("pretty printing indents properly") {
        val json = JsonObject(
            "x" -> JsonNumber(1),
            "y" -> JsonNumber(2)
        )
        val jsonString = "{\n    \"x\": 1,\n    \"y\": 2\n}"
        assert(Json.write(json, Some("    ")) === jsonString)
        assert(Json.write(json, None) === jsonString.replace(" ", "").replace("\n", ""))
    }

    test("labels without quotes must cause a parse error at the correct location") {
        val e = intercept[ParseJsonException] {
            Json.read("{\n    \"x\": 1,\n    y: 2\n}")
        }
        assert(e.line === 3)
        assert(e.column === 5)
    }

    test("untyped indexing works and returns None where appropriate") {
        assert(json7(1) === Some(json4))
        assert(json7(2) === None)
        assert(json11("z") === None)
        assert(json11("y") === Some(json10))
        assert(json11("y", "a") === Some(json3))
    }

    test("untyped access works") {
        assert(json1.isNull)
        assert(json2.boolean === Some(true))
        assert(json0.number === Some(0.0))
        assert(json4.string === Some("Hello \r\n\t\u0000\u0001\"\\ World! Æø"))
        assert(json5.elements === Some(List()))
        assert(json8.members === Some(Map()))
    }

    test("implicit primitive conversions should work") {
        import ConvertJson._
        val j = JsonObject(
            "address" -> JsonObject("city" -> "Copenhagen"),
            "luckyNumbers" -> JsonArray(7, 13, 42)
        )
        assert(j("address", "city").flatMap(_.string) === Some("Copenhagen"))
        assert(((null : String) : Json) === JsonNull)
        assert((true : Json) === JsonBoolean(true))
        assert((5 : Json) === JsonNumber(5))
        assert((3.14 : Json) === JsonNumber(3.14))
        assert(("foo" : Json) === JsonString("foo"))
    }

    /*
    // This isn't really a test, it's more like a very naive benchmark. However, it does pass on my machine.
    // TODO: Write a proper benchmark and publish it
    test("must quickly load a 22 megabyte file") {
        val before = System.currentTimeMillis()
        val json = Json.read(new File("documents.json"))
        val after = System.currentTimeMillis()
        assert(after - before < 10 * 1000)
    }
    */
}
