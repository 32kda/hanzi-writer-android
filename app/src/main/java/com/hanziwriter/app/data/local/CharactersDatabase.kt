package com.hanziwriter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hanziwriter.app.data.local.dao.CharacterDao
import com.hanziwriter.app.data.local.entity.CharacterEntity
import com.hanziwriter.app.data.local.entity.StrokeDataEntity

@Database(
    entities = [
        CharacterEntity::class,
        StrokeDataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CharactersDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}
