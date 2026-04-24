package com.example.alearning.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    fun parse(inputStream: InputStream): List<Map<String, String>> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine() ?: return emptyList()
        val headers = parseLine(headerLine)

        val result = mutableListOf<Map<String, String>>()
        var line: String? = reader.readLine()
        while (line != null) {
            val values = parseLine(line)
            if (values.size == headers.size) {
                val row = headers.zip(values).toMap()
                result.add(row)
            }
            line = reader.readLine()
        }
        return result
    }

    private fun parseLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var currentToken = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> {
                    tokens.add(currentToken.toString().trim())
                    currentToken = StringBuilder()
                }
                else -> {
                    currentToken.append(c)
                }
            }
            i++
        }
        tokens.add(currentToken.toString().trim())
        return tokens
    }
}
