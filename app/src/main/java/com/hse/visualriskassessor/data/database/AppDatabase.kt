package com.hse.visualriskassessor.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hse.visualriskassessor.data.converter.DateConverter
import com.hse.visualriskassessor.data.converter.HazardListConverter
import com.hse.visualriskassessor.data.converter.RiskLevelConverter
import com.hse.visualriskassessor.data.dao.AssessmentDao
import com.hse.visualriskassessor.data.entity.AssessmentEntity

@Database(entities = [AssessmentEntity::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class, HazardListConverter::class, RiskLevelConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assessmentDao(): AssessmentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hse_assessments.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
