package com.servicesphere.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS job_reminders (
                id TEXT NOT NULL PRIMARY KEY,
                jobId TEXT NOT NULL,
                reminderType TEXT NOT NULL,
                reminderTimeMillis INTEGER NOT NULL,
                isEnabled INTEGER NOT NULL,
                hasFired INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_job_reminders_jobId ON job_reminders(jobId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_job_reminders_reminderTimeMillis ON job_reminders(reminderTimeMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_job_reminders_isEnabled ON job_reminders(isEnabled)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_job_reminders_hasFired ON job_reminders(hasFired)")
    }
}

/** Keeps existing independent jobs valid while recording future quote conversions. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE jobs ADD COLUMN sourceQuoteId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_jobs_sourceQuoteId ON jobs(sourceQuoteId)")
    }
}
