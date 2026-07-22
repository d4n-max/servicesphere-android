package com.servicesphere.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusinessProfileEntity::class,
        ClientEntity::class,
        JobEntity::class,
        QuoteEntity::class,
        InvoiceEntity::class,
        LineItemEntity::class,
        JobPhotoEntity::class,
        SignatureEntity::class,
        JobReminderEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun jobDao(): JobDao
    abstract fun jobReminderDao(): JobReminderDao
    abstract fun quoteDao(): QuoteDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun lineItemDao(): LineItemDao
    abstract fun jobPhotoDao(): JobPhotoDao
    abstract fun signatureDao(): SignatureDao
}
