package com.hanziwriter.app.data.repository

import com.hanziwriter.app.data.local.dao.CharacterDao
import com.hanziwriter.app.data.local.entity.CharacterEntity
import com.hanziwriter.app.domain.model.character.Character
import com.hanziwriter.app.domain.model.geometry.BinaryPathParser
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
            val medianPoints = BinaryPathParser.parseMedians(strokeEntity.medianPoints)
            com.hanziwriter.app.domain.model.character.Stroke(
                path = strokeEntity.pathData,
                points = medianPoints,
                strokeNum = strokeEntity.strokeIndex
            ).also { it.parsePath(12) }
        }
        return Character(
            symbol = entity.character,
            strokes = strokes,
            pinyin = entity.pinyin,
            definition = entity.definition
        )
    }
}
