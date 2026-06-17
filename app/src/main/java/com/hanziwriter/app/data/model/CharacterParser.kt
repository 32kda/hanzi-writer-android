package com.hanziwriter.app.data.model

import com.hanziwriter.app.domain.model.character.Character
import com.hanziwriter.app.domain.model.character.Stroke
import com.hanziwriter.app.domain.model.geometry.Point
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CharacterParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseFromJson(symbol: String, jsonString: String): Character {
        val obj = json.parseToJsonElement(jsonString).jsonObject
        val strokesArray = obj["strokes"]!!.jsonArray
        val mediansArray = obj["medians"]!!.jsonArray

        val strokes = strokesArray.mapIndexed { i, _ ->
            val path = strokesArray[i].jsonPrimitive.content
            val medianPoints = mediansArray[i].jsonArray.map {
                val arr = it.jsonArray
                Point(arr[0].jsonPrimitive!!.content.toDouble(), arr[1].jsonPrimitive!!.content.toDouble())
            }
            val isInRadical = obj["radStrokes"]
                ?.jsonArray?.any { it.jsonPrimitive.content.toIntOrNull() == i } == true

            Stroke(path = path, points = medianPoints, strokeNum = i, isInRadical = isInRadical)
                .also { it.parseSvgPath(12) }
        }

        return Character(symbol, strokes)
    }
}
