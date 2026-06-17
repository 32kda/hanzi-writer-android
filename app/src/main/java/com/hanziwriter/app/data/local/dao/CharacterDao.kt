package com.hanziwriter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.hanziwriter.app.data.local.entity.CharacterEntity
import com.hanziwriter.app.data.local.entity.StrokeDataEntity

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters WHERE char = :character LIMIT 1")
    suspend fun getCharacter(character: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE unicode = :unicode")
    suspend fun getCharacterByUnicode(unicode: Int): CharacterEntity?

    @Query("SELECT * FROM stroke_data WHERE unicode = :unicode ORDER BY stroke_index")
    suspend fun getStrokesForCharacter(unicode: Int): List<StrokeDataEntity>
}
