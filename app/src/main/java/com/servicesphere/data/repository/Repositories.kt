package com.servicesphere.data.repository

import com.servicesphere.data.local.BusinessProfileDao
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientDao
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceDao
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobDao
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.JobPhotoDao
import com.servicesphere.data.local.JobPhotoEntity
import com.servicesphere.data.local.JobReminderDao
import com.servicesphere.data.local.JobReminderEntity
import com.servicesphere.data.local.LineItemDao
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteDao
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.local.SignatureDao
import com.servicesphere.data.local.SignatureEntity
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.QuoteStatus
import kotlinx.coroutines.flow.Flow

class BusinessRepository(private val dao: BusinessProfileDao) {
    fun observeBusinessProfile(): Flow<BusinessProfileEntity?> = dao.observeBusinessProfile()
    suspend fun getBusinessProfileOnce(): BusinessProfileEntity? = dao.getBusinessProfileOnce()
    suspend fun upsertBusinessProfile(profile: BusinessProfileEntity) = dao.upsertBusinessProfile(profile)
    suspend fun updateBusinessProfile(profile: BusinessProfileEntity) = dao.updateBusinessProfile(profile)
    suspend fun createDefaultBusinessProfileIfMissing(): BusinessProfileEntity {
        val existing = dao.getBusinessProfileOnce()
        if (existing != null) return existing
        val profile = BusinessProfileEntity(businessName = "ServiceSphere Business")
        dao.upsertBusinessProfile(profile)
        return profile
    }
}

class ClientRepository(private val dao: ClientDao) {
    fun observeClients(): Flow<List<ClientEntity>> = dao.observeClients()
    fun observeClientById(id: String): Flow<ClientEntity?> = dao.observeClientById(id)
    fun searchClients(query: String): Flow<List<ClientEntity>> = dao.searchClients(query)
    suspend fun insertClient(client: ClientEntity) = dao.insertClient(client)
    suspend fun updateClient(client: ClientEntity) = dao.updateClient(client)
    suspend fun deleteClient(client: ClientEntity) = dao.deleteClient(client)
    suspend fun getClientByIdOnce(id: String): ClientEntity? = dao.getClientByIdOnce(id)
    fun observeClientCount(): Flow<Int> = dao.observeClientCount()
    suspend fun getClientCountOnce(): Int = dao.getClientCountOnce()
}

class JobRepository(private val dao: JobDao) {
    fun observeJobs(): Flow<List<JobEntity>> = dao.observeJobs()
    fun observeJobById(id: String): Flow<JobEntity?> = dao.observeJobById(id)
    fun observeJobsByClient(clientId: String): Flow<List<JobEntity>> = dao.observeJobsByClient(clientId)
    fun observeJobsByStatus(status: String): Flow<List<JobEntity>> = dao.observeJobsByStatus(status)
    fun searchJobs(query: String): Flow<List<JobEntity>> = dao.searchJobs(query)
    fun observeTodayJobs(startOfDay: Long, endOfDay: Long): Flow<List<JobEntity>> = dao.observeTodayJobs(startOfDay, endOfDay)
    suspend fun insertJob(job: JobEntity) = dao.insertJob(job)
    suspend fun updateJob(job: JobEntity) = dao.updateJob(job)
    suspend fun deleteJob(job: JobEntity) = dao.deleteJob(job)
    suspend fun getJobByIdOnce(id: String): JobEntity? = dao.getJobByIdOnce(id)
    fun observeJobCount(): Flow<Int> = dao.observeJobCount()
}

class JobReminderRepository(private val dao: JobReminderDao) {
    fun observeAllReminders(): Flow<List<JobReminderEntity>> = dao.observeAllReminders()
    fun observeRemindersForJob(jobId: String): Flow<List<JobReminderEntity>> = dao.observeRemindersForJob(jobId)
    fun observeUpcomingEnabledReminders(now: Long): Flow<List<JobReminderEntity>> = dao.observeUpcomingEnabledReminders(now)
    suspend fun getReminderByIdOnce(id: String): JobReminderEntity? = dao.getReminderByIdOnce(id)
    suspend fun getFirstReminderForJobOnce(jobId: String): JobReminderEntity? = dao.getFirstReminderForJobOnce(jobId)

    suspend fun createOrUpdateReminderForJob(jobId: String, type: String, scheduledAt: Long, customTimeMillis: Long? = null): JobReminderEntity {
        val reminderTime = com.servicesphere.reminders.ReminderTimeCalculator.calculate(scheduledAt, type, customTimeMillis)
        require(reminderTime >= System.currentTimeMillis()) { "Reminder time is in the past." }
        val now = System.currentTimeMillis()
        val existing = dao.getFirstReminderForJobOnce(jobId)
        val reminder = existing?.copy(
            reminderType = type,
            reminderTimeMillis = reminderTime,
            isEnabled = true,
            hasFired = false,
            updatedAt = now
        ) ?: JobReminderEntity(
            jobId = jobId,
            reminderType = type,
            reminderTimeMillis = reminderTime,
            createdAt = now,
            updatedAt = now
        )
        if (existing == null) dao.insertReminder(reminder) else dao.updateReminder(reminder)
        return reminder
    }

    suspend fun updateReminder(reminder: JobReminderEntity) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: JobReminderEntity) = dao.deleteReminder(reminder)
    suspend fun deleteRemindersForJob(jobId: String) = dao.deleteRemindersForJob(jobId)
    suspend fun disableRemindersForJob(jobId: String) = dao.disableRemindersForJob(jobId, System.currentTimeMillis())
}

class QuoteRepository(private val dao: QuoteDao) {
    fun observeQuotes(): Flow<List<QuoteEntity>> = dao.observeQuotes()
    fun observeQuoteById(id: String): Flow<QuoteEntity?> = dao.observeQuoteById(id)
    fun observeQuotesByClient(clientId: String): Flow<List<QuoteEntity>> = dao.observeQuotesByClient(clientId)
    fun observeQuotesByJob(jobId: String): Flow<List<QuoteEntity>> = dao.observeQuotesByJob(jobId)
    fun observeQuotesByStatus(status: String): Flow<List<QuoteEntity>> = dao.observeQuotesByStatus(status)
    fun searchQuotes(query: String): Flow<List<QuoteEntity>> = dao.searchQuotes(query)
    suspend fun insertQuote(quote: QuoteEntity) = dao.insertQuote(quote)
    suspend fun updateQuote(quote: QuoteEntity) = dao.updateQuote(quote)
    suspend fun deleteQuote(quote: QuoteEntity) = dao.deleteQuote(quote)
    suspend fun getQuoteByIdOnce(id: String): QuoteEntity? = dao.getQuoteByIdOnce(id)
    fun observeDraftQuoteCount(): Flow<Int> = dao.observeQuoteCountByStatus(QuoteStatus.DRAFT)
}

class InvoiceRepository(private val dao: InvoiceDao) {
    fun observeInvoices(): Flow<List<InvoiceEntity>> = dao.observeInvoices()
    fun observeInvoiceById(id: String): Flow<InvoiceEntity?> = dao.observeInvoiceById(id)
    fun observeInvoicesByClient(clientId: String): Flow<List<InvoiceEntity>> = dao.observeInvoicesByClient(clientId)
    fun observeInvoicesByJob(jobId: String): Flow<List<InvoiceEntity>> = dao.observeInvoicesByJob(jobId)
    fun observeInvoicesByStatus(status: String): Flow<List<InvoiceEntity>> = dao.observeInvoicesByStatus(status)
    fun searchInvoices(query: String): Flow<List<InvoiceEntity>> = dao.searchInvoices(query)
    fun observeUnpaidInvoices(): Flow<List<InvoiceEntity>> = dao.observeUnpaidInvoices()
    fun observeOverdueInvoices(now: Long): Flow<List<InvoiceEntity>> = dao.observeOverdueInvoices(now)
    suspend fun insertInvoice(invoice: InvoiceEntity) = dao.insertInvoice(invoice)
    suspend fun updateInvoice(invoice: InvoiceEntity) = dao.updateInvoice(invoice)
    suspend fun deleteInvoice(invoice: InvoiceEntity) = dao.deleteInvoice(invoice)
    suspend fun getInvoiceByIdOnce(id: String): InvoiceEntity? = dao.getInvoiceByIdOnce(id)
    fun observeUnpaidInvoiceCount(): Flow<Int> = dao.observeUnpaidInvoiceCount()
}

class LineItemRepository(private val dao: LineItemDao) {
    fun observeLineItems(parentId: String, parentType: String): Flow<List<LineItemEntity>> = dao.observeLineItems(parentId, parentType)
    suspend fun insertLineItem(item: LineItemEntity) = dao.insertLineItem(item)
    suspend fun insertLineItems(items: List<LineItemEntity>) = dao.insertLineItems(items)
    suspend fun updateLineItem(item: LineItemEntity) = dao.updateLineItem(item)
    suspend fun deleteLineItem(item: LineItemEntity) = dao.deleteLineItem(item)
    suspend fun deleteLineItemsForParent(parentId: String, parentType: String) = dao.deleteLineItemsForParent(parentId, parentType)
}

class JobPhotoRepository(private val dao: JobPhotoDao) {
    fun observePhotosForJob(jobId: String): Flow<List<JobPhotoEntity>> = dao.observePhotosForJob(jobId)
    suspend fun insertPhoto(photo: JobPhotoEntity) = dao.insertPhoto(photo)
    suspend fun updatePhoto(photo: JobPhotoEntity) = dao.updatePhoto(photo)
    suspend fun deletePhoto(photo: JobPhotoEntity) = dao.deletePhoto(photo)
    suspend fun getPhotoByIdOnce(photoId: String): JobPhotoEntity? = dao.getPhotoByIdOnce(photoId)
}

class SignatureRepository(private val dao: SignatureDao) {
    fun observeSignaturesForJob(jobId: String): Flow<List<SignatureEntity>> = dao.observeSignaturesForJob(jobId)
    fun observeSignaturesForInvoice(invoiceId: String): Flow<List<SignatureEntity>> = dao.observeSignaturesForInvoice(invoiceId)
    suspend fun insertSignature(signature: SignatureEntity) = dao.insertSignature(signature)
    suspend fun updateSignature(signature: SignatureEntity) = dao.updateSignature(signature)
    suspend fun deleteSignature(signature: SignatureEntity) = dao.deleteSignature(signature)
    suspend fun getSignatureByIdOnce(signatureId: String): SignatureEntity? = dao.getSignatureByIdOnce(signatureId)
    fun observeSignaturesThisMonth(start: Long, end: Long): Flow<Int> = dao.observeSignaturesThisMonth(start, end)
}
