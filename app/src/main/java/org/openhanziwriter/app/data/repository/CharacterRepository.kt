package org.openhanziwriter.app.data.repository

import org.openhanziwriter.app.data.local.dao.CharacterDao
import org.openhanziwriter.app.data.local.entity.CharacterEntity
import org.openhanziwriter.app.domain.model.character.Character
import org.openhanziwriter.app.domain.model.geometry.BinaryPathParser
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

    fun buildDomainCharacter(
        entity: CharacterEntity,
        strokeEntities: List<org.openhanziwriter.app.data.local.entity.StrokeDataEntity>,
        pinyin: String = "",
        definition: String = ""
    ): Character {
        val strokes = strokeEntities.map { strokeEntity ->
            val medianPoints = BinaryPathParser.parseMedians(strokeEntity.medianPoints)
            org.openhanziwriter.app.domain.model.character.Stroke(
                path = strokeEntity.pathData,
                points = medianPoints,
                strokeNum = strokeEntity.strokeIndex
            ).also { it.parsePath(12) }
        }
        return Character(
            symbol = entity.character,
            strokes = strokes,
            pinyin = pinyin,
            definition = definition
        )
    }
}
