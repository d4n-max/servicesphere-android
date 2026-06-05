package com.servicesphere.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profiles WHERE id = 'default_business' LIMIT 1")
    fun observeBusinessProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles WHERE id = 'default_business' LIMIT 1")
    suspend fun getBusinessProfileOnce(): BusinessProfileEntity?

    @Upsert
    suspend fun upsertBusinessProfile(profile: BusinessProfileEntity)

    @Update
    suspend fun updateBusinessProfile(profile: BusinessProfileEntity)

    @Query("DELETE FROM business_profiles")
    suspend fun deleteAll()
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    fun observeClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun observeClientById(id: String): Flow<ClientEntity?>

    @Query(
        """
        SELECT * FROM clients
        WHERE name LIKE '%' || :query || '%'
           OR phone LIKE '%' || :query || '%'
           OR email LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        """
    )
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Insert
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientByIdOnce(id: String): ClientEntity?

    @Query("SELECT COUNT(*) FROM clients")
    fun observeClientCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getClientCountOnce(): Int

    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    suspend fun getAllClientsOnce(): List<ClientEntity>

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY COALESCE(scheduledAt, createdAt) DESC")
    fun observeJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    fun observeJobById(id: String): Flow<JobEntity?>

    @Query("SELECT * FROM jobs WHERE clientId = :clientId ORDER BY COALESCE(scheduledAt, createdAt) DESC")
    fun observeJobsByClient(clientId: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE status = :status ORDER BY COALESCE(scheduledAt, createdAt) DESC")
    fun observeJobsByStatus(status: String): Flow<List<JobEntity>>

    @Query(
        """
        SELECT * FROM jobs
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
           OR status LIKE '%' || :query || '%'
        ORDER BY COALESCE(scheduledAt, createdAt) DESC
        """
    )
    fun searchJobs(query: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE scheduledAt BETWEEN :startOfDay AND :endOfDay ORDER BY scheduledAt")
    fun observeTodayJobs(startOfDay: Long, endOfDay: Long): Flow<List<JobEntity>>

    @Insert
    suspend fun insertJob(job: JobEntity)

    @Update
    suspend fun updateJob(job: JobEntity)

    @Delete
    suspend fun deleteJob(job: JobEntity)

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    suspend fun getJobByIdOnce(id: String): JobEntity?

    @Query("SELECT COUNT(*) FROM jobs")
    fun observeJobCount(): Flow<Int>

    @Query("SELECT * FROM jobs ORDER BY COALESCE(scheduledAt, createdAt) DESC")
    suspend fun getAllJobsOnce(): List<JobEntity>

    @Query("DELETE FROM jobs")
    suspend fun deleteAll()
}

@Dao
interface JobReminderDao {
    @Query("SELECT * FROM job_reminders ORDER BY reminderTimeMillis")
    fun observeAllReminders(): Flow<List<JobReminderEntity>>

    @Query("SELECT * FROM job_reminders WHERE jobId = :jobId ORDER BY reminderTimeMillis")
    fun observeRemindersForJob(jobId: String): Flow<List<JobReminderEntity>>

    @Query("SELECT * FROM job_reminders WHERE isEnabled = 1 AND hasFired = 0 AND reminderTimeMillis >= :now ORDER BY reminderTimeMillis")
    fun observeUpcomingEnabledReminders(now: Long): Flow<List<JobReminderEntity>>

    @Query("SELECT * FROM job_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderByIdOnce(id: String): JobReminderEntity?

    @Query("SELECT * FROM job_reminders WHERE jobId = :jobId ORDER BY reminderTimeMillis LIMIT 1")
    suspend fun getFirstReminderForJobOnce(jobId: String): JobReminderEntity?

    @Insert
    suspend fun insertReminder(reminder: JobReminderEntity)

    @Update
    suspend fun updateReminder(reminder: JobReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: JobReminderEntity)

    @Query("DELETE FROM job_reminders WHERE jobId = :jobId")
    suspend fun deleteRemindersForJob(jobId: String)

    @Query("UPDATE job_reminders SET isEnabled = 0, updatedAt = :now WHERE jobId = :jobId")
    suspend fun disableRemindersForJob(jobId: String, now: Long)

    @Query("SELECT * FROM job_reminders ORDER BY reminderTimeMillis")
    suspend fun getAllRemindersOnce(): List<JobReminderEntity>

    @Query("DELETE FROM job_reminders")
    suspend fun deleteAll()
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY issueDate DESC")
    fun observeQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    fun observeQuoteById(id: String): Flow<QuoteEntity?>

    @Query("SELECT * FROM quotes WHERE clientId = :clientId ORDER BY issueDate DESC")
    fun observeQuotesByClient(clientId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE jobId = :jobId ORDER BY issueDate DESC")
    fun observeQuotesByJob(jobId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE status = :status ORDER BY issueDate DESC")
    fun observeQuotesByStatus(status: String): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM quotes
        WHERE quoteNumber LIKE '%' || :query || '%'
           OR status LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY issueDate DESC
        """
    )
    fun searchQuotes(query: String): Flow<List<QuoteEntity>>

    @Insert
    suspend fun insertQuote(quote: QuoteEntity)

    @Update
    suspend fun updateQuote(quote: QuoteEntity)

    @Delete
    suspend fun deleteQuote(quote: QuoteEntity)

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getQuoteByIdOnce(id: String): QuoteEntity?

    @Query("SELECT COUNT(*) FROM quotes WHERE status = :status")
    fun observeQuoteCountByStatus(status: String): Flow<Int>

    @Query("SELECT * FROM quotes ORDER BY issueDate DESC")
    suspend fun getAllQuotesOnce(): List<QuoteEntity>

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC")
    fun observeInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun observeInvoiceById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE clientId = :clientId ORDER BY issueDate DESC")
    fun observeInvoicesByClient(clientId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE jobId = :jobId ORDER BY issueDate DESC")
    fun observeInvoicesByJob(jobId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY issueDate DESC")
    fun observeInvoicesByStatus(status: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * FROM invoices
        WHERE invoiceNumber LIKE '%' || :query || '%'
           OR status LIKE '%' || :query || '%'
           OR paymentMethod LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY issueDate DESC
        """
    )
    fun searchInvoices(query: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status IN ('UNPAID', 'SENT', 'OVERDUE') ORDER BY dueDate")
    fun observeUnpaidInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status != 'PAID' AND dueDate IS NOT NULL AND dueDate < :now ORDER BY dueDate")
    fun observeOverdueInvoices(now: Long): Flow<List<InvoiceEntity>>

    @Insert
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdOnce(id: String): InvoiceEntity?

    @Query("SELECT COUNT(*) FROM invoices WHERE status IN ('UNPAID', 'SENT', 'OVERDUE')")
    fun observeUnpaidInvoiceCount(): Flow<Int>

    @Query("SELECT * FROM invoices ORDER BY issueDate DESC")
    suspend fun getAllInvoicesOnce(): List<InvoiceEntity>

    @Query("DELETE FROM invoices")
    suspend fun deleteAll()
}

@Dao
interface LineItemDao {
    @Query("SELECT * FROM line_items WHERE parentId = :parentId AND parentType = :parentType ORDER BY sortOrder")
    fun observeLineItems(parentId: String, parentType: String): Flow<List<LineItemEntity>>

    @Insert
    suspend fun insertLineItem(item: LineItemEntity)

    @Insert
    suspend fun insertLineItems(items: List<LineItemEntity>)

    @Update
    suspend fun updateLineItem(item: LineItemEntity)

    @Delete
    suspend fun deleteLineItem(item: LineItemEntity)

    @Query("DELETE FROM line_items WHERE parentId = :parentId AND parentType = :parentType")
    suspend fun deleteLineItemsForParent(parentId: String, parentType: String)

    @Query("SELECT * FROM line_items ORDER BY parentType, parentId, sortOrder")
    suspend fun getAllLineItemsOnce(): List<LineItemEntity>

    @Query("DELETE FROM line_items")
    suspend fun deleteAll()
}

@Dao
interface JobPhotoDao {
    @Query("SELECT * FROM job_photos WHERE jobId = :jobId ORDER BY createdAt DESC")
    fun observePhotosForJob(jobId: String): Flow<List<JobPhotoEntity>>

    @Insert
    suspend fun insertPhoto(photo: JobPhotoEntity)

    @Update
    suspend fun updatePhoto(photo: JobPhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: JobPhotoEntity)

    @Query("SELECT * FROM job_photos WHERE id = :photoId LIMIT 1")
    suspend fun getPhotoByIdOnce(photoId: String): JobPhotoEntity?

    @Query("SELECT * FROM job_photos ORDER BY createdAt DESC")
    suspend fun getAllPhotosOnce(): List<JobPhotoEntity>

    @Query("DELETE FROM job_photos")
    suspend fun deleteAll()
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures WHERE jobId = :jobId ORDER BY createdAt DESC")
    fun observeSignaturesForJob(jobId: String): Flow<List<SignatureEntity>>

    @Query("SELECT * FROM signatures WHERE invoiceId = :invoiceId ORDER BY createdAt DESC")
    fun observeSignaturesForInvoice(invoiceId: String): Flow<List<SignatureEntity>>

    @Insert
    suspend fun insertSignature(signature: SignatureEntity)

    @Update
    suspend fun updateSignature(signature: SignatureEntity)

    @Delete
    suspend fun deleteSignature(signature: SignatureEntity)

    @Query("SELECT * FROM signatures WHERE id = :signatureId LIMIT 1")
    suspend fun getSignatureByIdOnce(signatureId: String): SignatureEntity?

    @Query("SELECT COUNT(*) FROM signatures WHERE createdAt >= :start AND createdAt < :end")
    fun observeSignaturesThisMonth(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    suspend fun getAllSignaturesOnce(): List<SignatureEntity>

    @Query("DELETE FROM signatures")
    suspend fun deleteAll()
}
