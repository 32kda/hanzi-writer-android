package com.hanziwriter.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val unicode: Int,
    @ColumnInfo(name = "char") val character: String,
    @ColumnInfo(name = "pinyin") val pinyin: String = "",
    @ColumnInfo(name = "definition") val definition: String = ""
)
