package com.hanziwriter.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "stroke_data",
    primaryKeys = ["unicode", "stroke_index"]
)
data class StrokeDataEntity(
    val unicode: Int,
    @ColumnInfo(name = "stroke_index") val strokeIndex: Int,
    @ColumnInfo(name = "path_data") val pathData: String,
    @ColumnInfo(name = "median_points") val medianPoints: String
)
