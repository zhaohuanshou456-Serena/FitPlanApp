package com.fitplan.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitplan.app.data.dao.BodyRecordDao
import com.fitplan.app.data.dao.ExerciseDao
import com.fitplan.app.data.dao.ScheduledExerciseDao
import com.fitplan.app.data.entity.BodyRecord
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.entity.ScheduledExercise
import com.fitplan.app.data.program.ExerciseProgression
import com.fitplan.app.data.program.ProgressionDao
import com.fitplan.app.data.program.Program
import com.fitplan.app.data.program.ProgramDao
import com.fitplan.app.data.program.ProgramDayApply
import com.fitplan.app.data.program.ProgramDayApplyDao
import com.fitplan.app.data.program.ProgramItem
import com.fitplan.app.data.program.ProgramItemDao
import com.fitplan.app.data.program.ProgramSession
import com.fitplan.app.data.program.ProgramSessionDao
import com.fitplan.app.data.workout.WorkoutDao
import com.fitplan.app.data.workout.WorkoutSession
import com.fitplan.app.data.workout.WorkoutSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        ScheduledExercise::class,
        BodyRecord::class,
        Program::class,
        ProgramSession::class,
        ProgramItem::class,
        ProgramDayApply::class,
        ExerciseProgression::class,
        WorkoutSession::class,
        WorkoutSet::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FitPlanDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun scheduledExerciseDao(): ScheduledExerciseDao
    abstract fun bodyRecordDao(): BodyRecordDao

    abstract fun programDao(): ProgramDao
    abstract fun programSessionDao(): ProgramSessionDao
    abstract fun programItemDao(): ProgramItemDao
    abstract fun programDayApplyDao(): ProgramDayApplyDao
    abstract fun progressionDao(): ProgressionDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "fitplan.db"

        /** v1 -> v2：新增「可复用训练方案」相关的 5 张表（不影响既有数据）。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `goal` TEXT, " +
                        "`dayCount` INTEGER NOT NULL, `note` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programId` INTEGER NOT NULL, `sessionOrder` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `note` TEXT, " +
                        "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_sessions_programId` ON `program_sessions` (`programId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, " +
                        "`itemOrder` INTEGER NOT NULL, `targetSets` INTEGER NOT NULL, " +
                        "`repMin` INTEGER NOT NULL, `repMax` INTEGER NOT NULL, " +
                        "`weightKg` REAL, `restSeconds` INTEGER NOT NULL, `formCue` TEXT, " +
                        "`enableProgressive` INTEGER NOT NULL, `note` TEXT, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `program_sessions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_items_sessionId` ON `program_items` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_items_exerciseId` ON `program_items` (`exerciseId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program_day_apply` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`dateEpochDay` INTEGER NOT NULL, `programId` INTEGER NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, `appliedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `program_sessions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_program_day_apply_dateEpochDay` ON `program_day_apply` (`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_day_apply_sessionId` ON `program_day_apply` (`sessionId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_progression` (" +
                        "`exerciseId` INTEGER NOT NULL PRIMARY KEY, " +
                        "`bestWeightKg` REAL, `bestReps` INTEGER, " +
                        "`suggestedWeightKg` REAL, `updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v2 -> v3：新增训练会话与组记录（开始训练/组间计时/历史）。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `endedAt` INTEGER, " +
                        "`title` TEXT, `sourceKind` TEXT NOT NULL, `sourceRef` INTEGER, " +
                        "`note` TEXT)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, " +
                        "`exerciseName` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, " +
                        "`setIndex` INTEGER NOT NULL, `weightKg` REAL, `reps` INTEGER NOT NULL, " +
                        "`plannedReps` INTEGER, `restSeconds` INTEGER, `loggedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionId` ON `workout_sets` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON `workout_sets` (`exerciseId`)")
            }
        }

        /** v3 -> v4：身体记录增加照片路径列（拍照/选图留档） */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `body_records` ADD COLUMN `photoPath` TEXT")
            }
        }

        /** v4 -> v5：增加身高/蛋白质/皮下脂肪列（贴合体测仪读数） */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `body_records` ADD COLUMN `heightCm` REAL")
                db.execSQL("ALTER TABLE `body_records` ADD COLUMN `proteinPct` REAL")
                db.execSQL("ALTER TABLE `body_records` ADD COLUMN `subcutaneousPct` REAL")
            }
        }

        @Volatile
        private var INSTANCE: FitPlanDatabase? = null

        fun get(context: Context, scope: CoroutineScope): FitPlanDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitPlanDatabase::class.java,
                    NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
