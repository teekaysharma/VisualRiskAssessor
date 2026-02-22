package com.hse.visualriskassessor.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hse.visualriskassessor.data.converter.AnalysisModeConverter
import com.hse.visualriskassessor.data.converter.DateConverter
import com.hse.visualriskassessor.data.converter.HazardListConverter
import com.hse.visualriskassessor.data.converter.RiskLevelConverter
import com.hse.visualriskassessor.data.dao.AssessmentDao
import com.hse.visualriskassessor.data.entity.AssessmentEntity

@Database(entities = [AssessmentEntity::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class, HazardListConverter::class, RiskLevelConverter::class, AnalysisModeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assessmentDao(): AssessmentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE assessments ADD COLUMN analysisMode TEXT NOT NULL DEFAULT 'ML_DETECTION'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hse_assessments.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
