package org.foundator.json

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.mutable
import com.fasterxml.jackson.core.`type`.TypeReference

object BenchmarkJson {
    def main(args : Array[String]) {
        import ConvertJson._

        val json = JsonObject(
            "large" -> JsonArray(
                (for(i <- 0 until 1000000) yield JsonNumber(i)) : _*
            )
        )
        //val file = File.createTempFile("benchmark", ".json")
        //Json.write(file, json, None)
        val out = new ByteArrayOutputStream()
        Json.write(out, json, None)
        val bytes = out.toByteArray

        val jackson = mutable.Buffer[Double]()
        val foundator = mutable.Buffer[Double]()

        val jacksonMapper = new ObjectMapper
        val jacksonType = new TypeReference[java.util.Map[java.lang.String, java.util.List[java.lang.Double]]] {}

        val rounds = 100
        for(i <- 0 to rounds) {
            val jacksonIn = new ByteArrayInputStream(bytes)
            val jacksonBefore = System.currentTimeMillis
            val jacksonResult : java.util.Map[java.lang.String, java.util.List[java.lang.Double]] = jacksonMapper.readValue(jacksonIn, jacksonType)
            val jacksonSpent = System.currentTimeMillis - jacksonBefore
            if(i != 0) jackson += jacksonSpent
            val jacksonTest = jacksonResult.get("large").get(1)
            if(jacksonTest != 1) throw new RuntimeException("Invalid deserialization by Jackson: " + jacksonTest)

            val foundatorIn = new ByteArrayInputStream(bytes)
            val foundatorBefore = System.currentTimeMillis
            val foundatorResult = Json.read(foundatorIn)
            val foundatorSpent = System.currentTimeMillis - foundatorBefore
            if(i != 0) foundator += foundatorSpent
            val foundatorTest = foundatorResult("large").flatMap(_(1))
            if(foundatorTest != Some[Json](1)) throw new RuntimeException("Invalid deserialization by foundator-json: " + foundatorTest)

            System.out.println("ROUND " + i + " OF " + rounds + (if(i != 0) "" else " (skipped as warm-up)"))
            System.out.println("Jackson spent: " + jacksonSpent + " ms")
            System.out.println("foundator-json spent: " + foundatorSpent + " ms")
        }

        val jacksonAverage = jackson.sum / jackson.length
        val foundatorAverage = foundator.sum / foundator.length

        System.out.println("AVERAGES")
        System.out.println("Jackson spent: " + jacksonAverage.round + " ms")
        System.out.println("foundator-json spent: " + foundatorAverage.round + " ms")
    }
}
