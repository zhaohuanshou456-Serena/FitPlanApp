package com.fitplan.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitplan.app.data.dao.BodyRecordDao
import com.fitplan.app.data.dao.ExerciseDao
import com.fitplan.app.data.dao.ScheduledExerciseDao
import com.fitplan.app.data.entity.BodyRecord
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.entity.ScheduledExercise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Exercise::class, ScheduledExercise::class, BodyRecord::class],
    version = 1,
    exportSchema = false
)
abstract class FitPlanDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun scheduledExerciseDao(): ScheduledExerciseDao
    abstract fun bodyRecordDao(): BodyRecordDao

    companion object {
        const val NAME = "fitplan.db"

        @Volatile
        private var INSTANCE: FitPlanDatabase? = null

        fun get(context: Context, scope: CoroutineScope): FitPlanDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitPlanDatabase::class.java,
                    NAME
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 首次创建时预置默认动作库
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.exerciseDao()
                                    ?.insertAll(DefaultExercises.all)
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
