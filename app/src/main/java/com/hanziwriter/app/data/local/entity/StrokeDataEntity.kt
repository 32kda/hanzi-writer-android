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
    @ColumnInfo(name = "path_data", typeAffinity = ColumnInfo.BLOB) val pathData: ByteArray,
    @ColumnInfo(name = "median_points", typeAffinity = ColumnInfo.BLOB) val medianPoints: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StrokeDataEntity

        if (unicode != other.unicode) return false
        if (strokeIndex != other.strokeIndex) return false
        if (!pathData.contentEquals(other.pathData)) return false
        if (!medianPoints.contentEquals(other.medianPoints)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unicode
        result = 31 * result + strokeIndex
        result = 31 * result + pathData.contentHashCode()
        result = 31 * result + medianPoints.contentHashCode()
        return result
    }
}
