package org.foundator.json

import java.io._
import java.nio.charset.Charset


/**
 * This represents all valid JSON values, and is modelled closely after the json.org specification (RFC 4627).
 * Note that the order of the members in the JsonObject is preserved, though this is not required by the specification.
 */
sealed abstract class Json
case class  JsonObject  ( members  : (String, Json)* ) extends Json
case class  JsonArray   ( elements : Json*           ) extends Json
case class  JsonString  ( text     : String          ) extends Json
case class  JsonNumber  ( number   : Double          ) extends Json
case class  JsonBoolean ( value    : Boolean         ) extends Json
case object JsonNull                                   extends Json


/**
 * Contains serialization (write) an deserialization (read) methods for the JSON format.
 * These match the specification very closely, and should neither produce nor accept invalid JSON.
 * The only exception from this is that some of the read() methods (only where documented) accepts a BOM.
 * The methods in this class are designed to be fast and produce high quality error messages.
 */
object Json {
    /**
     * Writes the json object to a String in the JSON format and returns it.
     * If the indentation is None, no whitespace will be added.
     * Otherwise, maps and arrays with multiple elements will be indented using the provided string.
     */
    def write(json : Json, indentation : Option[String]) : String = {
        val writer = new StringWriter()
        write(writer, json, indentation)
        writer.toString
    }

    /**
     * Writes the json object to the given OutputStream in the JSON format.
     * If the indentation is None, no whitespace will be added.
     * Otherwise, maps and arrays with multiple elements will be indented using the provided string.
     * This method uses the default JSON charset UTF-8.
     * If you'd like to use a different charset, please use the Writer overload.
     * To quote the JSON specification (RFC 4627):
     * "JSON text SHALL be encoded in Unicode. The default encoding is UTF-8."
     */
    def write(out : OutputStream, json : Json, indentation : Option[String]) {
        val writer = new BufferedWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")))
        write(writer, json, indentation)
    }

    /**
     * Writes the json object to a file in the JSON format.
     * If the indentation is None, no whitespace will be added.
     * Otherwise, maps and arrays with multiple elements will be indented using the provided string.
     * This method uses the default JSON charset UTF-8.
     * If you'd like to use a different charset, please use the Writer overload.
     * To quote the JSON specification (RFC 4627):
     * "JSON text SHALL be encoded in Unicode. The default encoding is UTF-8."
     */
    def write(file : File, json : Json, indentation : Option[String]) {
        val out = new FileOutputStream(file)
        try {
            write(out, json, indentation)
        } finally {
            out.close()
        }
    }

    /**
     * Writes the json object to the given Writer in the JSON format.
     * If the indentation is None, no whitespace will be added.
     * Otherwise, maps and arrays with multiple elements will be indented using the provided string.
     * Please consider the following quote from the JSON specification (RFC 4627) when choosing a charset:
     * "JSON text SHALL be encoded in Unicode. The default encoding is UTF-8."
     */
    def write(writer : Writer, json : Json, indentation : Option[String]) {
        val pretty = indentation.isDefined
        val indentationString = indentation.getOrElse(null)

        def writeString(text : String) {
            writer.write('"')
            for(c <- text) c match {
                case '\"' => writer.write("\\\"")
                case '\\' => writer.write("\\\\")
                case '\b' => writer.write("\\b")
                case '\f' => writer.write("\\f")
                case '\n' => writer.write("\\n")
                case '\r' => writer.write("\\r")
                case '\t' => writer.write("\\t")
                case _ if c.isControl => writer.write(f"\\u${c.toInt}%04x") // omg
                case _ => writer.write(c)
            }
            writer.write('"')
        }

        def writeLine(level : Int) {
            if(pretty) {
                writer.write('\n')
                for(_ <- 0 until level) writer.write(indentationString)
            }
        }

        def writeJson(json : Json, level : Int) {
            json match {
                case JsonObject() =>
                    writer.write("{}")
                case JsonObject((label : String, value)) =>
                    writer.write('{')
                    writeString(label)
                    writer.write(':')
                    if(pretty) writer.write(' ')
                    writeJson(value, level)
                    writer.write('}')
                case JsonObject(members @ _*) =>
                    writer.write('{')
                    var first = true
                    for((label : String, value : Json) <- members) {
                        if(first) {
                            first = false
                        } else {
                            writer.write(',')
                        }
                        writeLine(level + 1)
                        writeString(label)
                        writer.write(':')
                        if(pretty) writer.write(' ')
                        writeJson(value, level + 1)
                    }
                    writeLine(level)
                    writer.write('}')
                case JsonArray() =>
                    writer.write("[]")
                case JsonArray(element) =>
                    writer.write('[')
                    writeJson(element, level)
                    writer.write(']')
                case JsonArray(elements @ _*) =>
                    writer.write('[')
                    var first = true
                    for(element <- elements) {
                        if(first) {
                            first = false
                        } else {
                            writer.write(',')
                        }
                        writeLine(level + 1)
                        writeJson(element, level + 1)
                    }
                    writeLine(level)
                    writer.write(']')
                case JsonString(text) =>
                    writeString(text)
                case JsonNumber(number) =>
                    val text = number.toString
                    if(text.startsWith(".")) writer.write('0')
                    writer.write(text)
                case JsonBoolean(value) =>
                    writer.write(if(value) "true" else "false")
                case JsonNull =>
                    writer.write("null")
            }
        }

        writeJson(json, 0)
        writer.flush()
    }

    /**
     * Reads the Json object from the given InputStream in JSON format.
     * This method automatically guesses the encoding from the first 4 bytes, as mandated by the JSON specification:
     * UTF-8 (the default), UTF-16LE, UTF-16BE, UTF-32LE or UTF-32BE
     * This method allows a BOM at the beginning of the stream. This is required for compatibility, but is non-standard.
     * Please consider the following quote from the JSON specification (RFC 4627) when choosing a charset:
     * "JSON text SHALL be encoded in Unicode. The default encoding is UTF-8."
     */
    def read(in : InputStream) : Json = read(in, false)

    /**
     * Reads the Json object from the given InputStream in JSON format.
     * If allowMore is true (the is default false), additional characters are allowed after the JSON value.
     * This method automatically guesses the encoding from the first 4 bytes, as mandated by the JSON specification:
     * UTF-8 (the default), UTF-16LE, UTF-16BE, UTF-32LE or UTF-32BE
     * This method allows a BOM at the beginning of the stream. This is required for compatibility, but is non-standard.
     * Please consider the following quote from the JSON specification (RFC 4627) when choosing a charset:
     * "JSON text SHALL be encoded in Unicode. The default encoding is UTF-8."
     */
    def read(in : InputStream, allowMore : Boolean) : Json = {
        val input = new BufferedInputStream(in)
        input.mark(4)
        val bytes = new Array[Byte](4)
        var count = 0
        var done = false
        while(count < 4 && !done) {
            val i = input.read(bytes, count, 4 - count)
            if(i == -1) done = true
            else count += i
        }
        input.reset()
        val (skip, charset) = bytes.slice(0, count) match {
            case Array(0,    0,    0xFE, 0xFF) => (4, "UTF-32BE")  // BOM
            case Array(0xFF, 0xFE, 0,    0   ) => (4, "UTF-32LE")  // BOM
            case Array(0xFE, 0xFF, _,    _   ) => (2, "UTF-16BE")  // BOM
            case Array(0xFF, 0xFE, _,    _   ) => (2, "UTF-16LE")  // BOM
            case Array(0,    0,    0,    _   ) => (0, "UTF-32BE")  // One ASCII character
            case Array(0,    _,    0,    _   ) => (0, "UTF-16BE")  // Two ASCII characters
            case Array(_,    0,    0,    0   ) => (0, "UTF-32LE")  // One ASCII character
            case Array(_,    0,    _,    0   ) => (0, "UTF-16LE")  // Two ASCII characters
            case Array(0xEF, 0xBB, 0xBF, _   ) => (3, "UTF-8")     // BOM
            case _ => (0, "UTF-8")
        }
        for(i <- 0 until skip) input.read()
        read(new InputStreamReader(input, Charset.forName(charset)), allowMore)
    }

    /**
     * Reads the Json object from the given File in JSON format.
     * This method automatically guesses the encoding from the first 4 bytes, as mandated by the JSON specification:
     * UTF-8 (the default), UTF-16LE, UTF-16BE, UTF-32LE or UTF-32BE
     * This method allows a BOM at the beginning of the stream. This is required for compatibility, but is non-standard.
     * Please consider the following quote from the JSON specification (RFC 4627) when choosing a charset:
     * "JSON text SHALL be encoded in Unicode. The default encoding is UTF-8."
     */
    def read(file : File) : Json = {
        val in = new FileInputStream(file)
        try {
            read(in, allowMore = false)
        } finally {
            in.close()
        }
    }

    /**
     * Reads the Json object from the String.
     */
    def read(text : String) : Json = read(new StringReader(text), allowMore = false)

    /**
     * Reads the Json object from the Reader.
     */
    def read(reader : Reader) : Json = read(reader, allowMore = false)

    /**
     * Reads the Json object from the String.
     * If allowMore is true (the is default false), additional characters are allowed after the JSON value.
     */
    def read(reader : Reader, allowMore : Boolean) : Json = {

        var line = 1
        var column = 0

        var current : Int = -1
        def next() = {
            if(current == '\n') {
                line += 1
                column = 1
            } else {
                column += 1
            }
            current = reader.read()
            current
        }
        next()

        def readObject() : Json = {
            if(current != '{') throw ParseJsonException("Expected an object { (left brace)", line, column)
            next()
            skipWhitespace()
            var members = List[(String, Json)]()
            var first = true
            while(current != '}') {
                if(first) {
                    first = false
                } else {
                    if(current != ',') throw ParseJsonException("Expected a , (comma)", line, column)
                    next()
                    skipWhitespace()
                }
                if(current != '"') throw ParseJsonException("Expected a label", line, column)
                val label = readString()
                if(current != ':') throw ParseJsonException("Expected a : (colon)", line, column)
                next()
                skipWhitespace()
                val value = readJson()
                members = (label -> value) :: members
            }
            next()
            skipWhitespace()
            JsonObject(members.reverse : _*)
        }

        def readArray() : Json = {
            if(current != '[') throw ParseJsonException("Expected an array [ (left bracket)", line, column)
            next()
            skipWhitespace()
            var elements = List[Json]()
            var first = true
            while(current != ']') {
                if(first) {
                    first = false
                } else {
                    if(current != ',') throw ParseJsonException("Expected a , (comma)", line, column)
                    next()
                    skipWhitespace()
                }
                val value = readJson()
                elements = value :: elements
            }
            next()
            skipWhitespace()
            JsonArray(elements.reverse : _*)
        }

        def readString() : String = {
            if(current != '"') throw ParseJsonException("Expected a string", line, column)
            next()
            val builder = new StringBuilder()
            while(current != '"') {
                if(current == -1) throw ParseJsonException("Unexpected end of file inside a string", line, column)
                if(current.toChar.isControl) throw ParseJsonException("Unescaped control character inside a string", line, column)
                if(current == '\\') {
                    val result = next() match {
                        case -1 => throw ParseJsonException("Unexpected end of file inside an escape sequence", line, column)
                        case '"' => '"'
                        case '\\' => '\\'
                        case '/' => '/'
                        case 'b' => '\b'
                        case 'f' => '\f'
                        case 'n' => '\n'
                        case 'r' => '\r'
                        case 't' => '\t'
                        case 'u' =>
                            val hex1 = next()
                            val hex2 = next()
                            val hex3 = next()
                            val hex4 = next()
                            try {
                                Integer.parseInt("" + hex1.toChar + hex2.toChar + hex3.toChar + hex4.toChar, 16).toChar
                            } catch {
                                case e : NumberFormatException => throw ParseJsonException(e.getMessage, line, column)
                            }
                        case _ => throw ParseJsonException("Unknown escape sequence: \\" + current, line, column)
                    }
                    builder.append(result)
                } else {
                    builder.append(current.toChar)
                }
                next()
            }
            next()
            skipWhitespace()
            builder.toString()
        }

        def isNumberPart(c : Int) : Boolean =
            (c >= '0' && c <= '9') || c == '+' || c == '-' || c == 'e' || c == 'E' || c == '.'

        def readNumber() : Json = {
            val builder = new StringBuilder
            while(isNumberPart(current)) {
                builder.append(current.toChar)
                next()
            }
            skipWhitespace()
            val text = builder.toString()
            if(text.startsWith(".") || text.startsWith("-.") || text.startsWith("+")) throw ParseJsonException("A JSON number must have a digit before the . and can't start with a +", line, column)
            try {
                JsonNumber(java.lang.Double.parseDouble(text))
            } catch {
                case e : NumberFormatException => throw ParseJsonException(e.getMessage, line, column)
            }
        }

        def readTrue() : Json = {
            if(current != 't') throw ParseJsonException("Expected 'true'", line, column)
            if(next() != 'r') throw ParseJsonException("Expected 'true'", line, column)
            if(next() != 'u') throw ParseJsonException("Expected 'true'", line, column)
            if(next() != 'e') throw ParseJsonException("Expected 'true'", line, column)
            next()
            skipWhitespace()
            JsonBoolean(value = true)
        }

        def readFalse() : Json = {
            if(current != 'f') throw ParseJsonException("Expected 'false'", line, column)
            if(next() != 'a') throw ParseJsonException("Expected 'false'", line, column)
            if(next() != 'l') throw ParseJsonException("Expected 'false'", line, column)
            if(next() != 's') throw ParseJsonException("Expected 'false'", line, column)
            if(next() != 'e') throw ParseJsonException("Expected 'false'", line, column)
            next()
            skipWhitespace()
            JsonBoolean(value = false)
        }

        def readNull() : Json = {
            if(current != 'n') throw ParseJsonException("Expected 'null'", line, column)
            if(next() != 'u') throw ParseJsonException("Expected 'null'", line, column)
            if(next() != 'l') throw ParseJsonException("Expected 'null'", line, column)
            if(next() != 'l') throw ParseJsonException("Expected 'null'", line, column)
            next()
            skipWhitespace()
            JsonNull
        }

        def skipWhitespace() {
            while(Character.isWhitespace(current)) next()
        }

        def readJson() : Json = {
            current match {
                case '{' => readObject()
                case '[' => readArray()
                case '"' => JsonString(readString())
                case 't' => readTrue()
                case 'f' => readFalse()
                case 'n' => readNull()
                case c if isNumberPart(c) => readNumber()
                case -1 => throw ParseJsonException("Unexpected end of file", line, column)
                case _ => throw ParseJsonException("Unexpected character: " + current.toChar + " (" + current + ")", line, column)
            }
        }

        skipWhitespace()
        val result = readJson()
        if(!allowMore && current != -1)  throw ParseJsonException("Expected end of file after the JSON value", line, column)
        result
    }
}

case class ParseJsonException(message : String, line : Int, column : Int)
    extends RuntimeException(message + " at line " + line + ", column " + column)
