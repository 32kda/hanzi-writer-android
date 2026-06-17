package com.hanziwriter.app.data.repository

import com.hanziwriter.app.data.local.dao.CharacterDao
import com.hanziwriter.app.data.local.entity.CharacterEntity
import com.hanziwriter.app.domain.model.character.Character
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val characterDao: CharacterDao
) {
    suspend fun getCharacter(character: String): CharacterEntity? =
        characterDao.getCharacter(character)

    suspend fun getCharacterByUnicode(unicode: Int): CharacterEntity? =
        characterDao.getCharacterByUnicode(unicode)

    suspend fun getStrokeData(unicode: Int) =
        characterDao.getStrokesForCharacter(unicode)

    fun buildDomainCharacter(entity: CharacterEntity, strokeEntities: List<com.hanziwriter.app.data.local.entity.StrokeDataEntity>): Character {
        val strokes = strokeEntities.map { strokeEntity ->
            val medianPoints = parseMedianPoints(strokeEntity.medianPoints)
            com.hanziwriter.app.domain.model.character.Stroke(
                path = strokeEntity.pathData,
                points = medianPoints,
                strokeNum = strokeEntity.strokeIndex
            ).also { it.parseSvgPath(12) }
        }
        return Character(symbol = entity.character, strokes = strokes)
    }

    private fun parseMedianPoints(json: String): List<com.hanziwriter.app.domain.model.geometry.Point> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val pt = arr.getJSONArray(i)
                com.hanziwriter.app.domain.model.geometry.Point(
                    pt.getDouble(0),
                    pt.getDouble(1)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
