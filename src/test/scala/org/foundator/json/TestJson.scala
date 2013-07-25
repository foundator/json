package org.foundator.json

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.io.{ByteArrayInputStream, OutputStreamWriter, ByteArrayOutputStream}
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
}
