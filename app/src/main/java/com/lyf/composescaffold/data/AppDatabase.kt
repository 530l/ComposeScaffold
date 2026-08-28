package com.lyf.composescaffold.data

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

/**
 * Room 聚合数据库：Entity/Dao 放各 feature 的 data/local，
 * 但必须在此注册；升级走三件套铁律（递增 version + schema JSON + 显式 migration）。
 */
@Database(
    entities = [SchemaPlaceholderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase()

/** 占位实体：首个真实 Entity 落地时替换。 */
@Entity(tableName = "schema_placeholder")
data class SchemaPlaceholderEntity(
    @PrimaryKey val id: Long = 0,
)
