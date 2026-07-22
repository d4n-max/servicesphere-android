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

/** Adds durable document lifecycle metadata without altering existing records. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quotes ADD COLUMN sentAt INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN acceptedAt INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN declinedAt INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN convertedInvoiceId TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN pdfPath TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN pdfGeneratedAt INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN pdfSourceUpdatedAt INTEGER")
        db.execSQL("ALTER TABLE invoices ADD COLUMN sentAt INTEGER")
        db.execSQL("ALTER TABLE invoices ADD COLUMN voidedAt INTEGER")
        db.execSQL("ALTER TABLE invoices ADD COLUMN pdfPath TEXT")
        db.execSQL("ALTER TABLE invoices ADD COLUMN pdfGeneratedAt INTEGER")
        db.execSQL("ALTER TABLE invoices ADD COLUMN pdfSourceUpdatedAt INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS document_activity (id TEXT NOT NULL PRIMARY KEY, documentId TEXT NOT NULL, documentType TEXT NOT NULL, eventType TEXT NOT NULL, detail TEXT, relatedDocumentId TEXT, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_document_activity_documentId ON document_activity(documentId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_document_activity_documentType ON document_activity(documentType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_document_activity_createdAt ON document_activity(createdAt)")
    }
}
