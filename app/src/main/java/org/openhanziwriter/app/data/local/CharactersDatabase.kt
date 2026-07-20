package org.openhanziwriter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.openhanziwriter.app.data.local.dao.CharacterDao
import org.openhanziwriter.app.data.local.entity.CharacterEntity
import org.openhanziwriter.app.data.local.entity.StrokeDataEntity

@Database(
    entities = [
        CharacterEntity::class,
        StrokeDataEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CharactersDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}
