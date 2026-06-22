package com.servicesphere.data

import android.content.Context
import androidx.room.Room
import com.servicesphere.BuildConfig
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.activation.DebugActivationTracker
import com.servicesphere.billing.MockBillingService
import com.servicesphere.billing.FeatureGateManager
import com.servicesphere.billing.RevenueCatManager
import com.servicesphere.billing.SubscriptionService
import com.servicesphere.billing.SubscriptionRepository
import com.servicesphere.data.local.AppDatabase
import com.servicesphere.data.local.DemoDataSeeder
import com.servicesphere.data.local.MIGRATION_1_2
import com.servicesphere.data.export.DataExportManager
import com.servicesphere.data.preferences.UserPreferences
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.FakeServiceSphereRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobPhotoRepository
import com.servicesphere.data.repository.JobReminderRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.data.repository.ServiceSphereRepository
import com.servicesphere.data.repository.SignatureRepository
import com.servicesphere.pdf.PdfService
import com.servicesphere.pdf.ServiceSpherePdfService
import com.servicesphere.review.ReviewPromptManager
import com.servicesphere.share.AndroidShareService
import com.servicesphere.share.ShareService
import com.servicesphere.reminders.JobReminderScheduler

object ServiceLocator {
    lateinit var database: AppDatabase
        private set
    lateinit var preferences: UserPreferences
        private set
    lateinit var repository: ServiceSphereRepository
        private set
    lateinit var businessRepository: BusinessRepository
        private set
    lateinit var clientRepository: ClientRepository
        private set
    lateinit var jobRepository: JobRepository
        private set
    lateinit var quoteRepository: QuoteRepository
        private set
    lateinit var invoiceRepository: InvoiceRepository
        private set
    lateinit var lineItemRepository: LineItemRepository
        private set
    lateinit var jobPhotoRepository: JobPhotoRepository
        private set
    lateinit var jobReminderRepository: JobReminderRepository
        private set
    lateinit var signatureRepository: SignatureRepository
        private set
    lateinit var reminderScheduler: JobReminderScheduler
        private set
    lateinit var billing: SubscriptionService
        private set
    lateinit var subscriptionRepository: SubscriptionRepository
        private set
    lateinit var featureGateManager: FeatureGateManager
        private set
    lateinit var revenueCatManager: RevenueCatManager
        private set
    lateinit var pdf: PdfService
        private set
    lateinit var share: ShareService
        private set
    lateinit var dataExportManager: DataExportManager
        private set
    lateinit var reviewPromptManager: ReviewPromptManager
        private set
    lateinit var activationTracker: ActivationTracker
        private set
    private lateinit var seeder: DemoDataSeeder

    fun init(context: Context) {
        if (::preferences.isInitialized) return
        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "servicesphere.db"
        ).addMigrations(MIGRATION_1_2).build()
        preferences = UserPreferences(context)
        repository = FakeServiceSphereRepository()
        businessRepository = BusinessRepository(database.businessProfileDao())
        clientRepository = ClientRepository(database.clientDao())
        jobRepository = JobRepository(database.jobDao())
        quoteRepository = QuoteRepository(database.quoteDao())
        invoiceRepository = InvoiceRepository(database.invoiceDao())
        lineItemRepository = LineItemRepository(database.lineItemDao())
        jobPhotoRepository = JobPhotoRepository(database.jobPhotoDao())
        jobReminderRepository = JobReminderRepository(database.jobReminderDao())
        signatureRepository = SignatureRepository(database.signatureDao())
        reminderScheduler = JobReminderScheduler(context.applicationContext)
        reminderScheduler.ensureNotificationChannel()
        seeder = DemoDataSeeder(
            businessProfileDao = database.businessProfileDao(),
            clientDao = database.clientDao(),
            jobDao = database.jobDao(),
            quoteDao = database.quoteDao(),
            invoiceDao = database.invoiceDao(),
            lineItemDao = database.lineItemDao()
        )
        billing = MockBillingService()
        revenueCatManager = RevenueCatManager(BuildConfig.REVENUECAT_API_KEY)
        revenueCatManager.initialize(context.applicationContext)
        subscriptionRepository = SubscriptionRepository(preferences, revenueCatManager)
        featureGateManager = FeatureGateManager(
            subscriptionRepository = subscriptionRepository,
            clientRepository = clientRepository,
            jobRepository = jobRepository,
            quoteRepository = quoteRepository,
            invoiceRepository = invoiceRepository,
            jobPhotoRepository = jobPhotoRepository,
            signatureRepository = signatureRepository
        )
        pdf = ServiceSpherePdfService()
        share = AndroidShareService()
        activationTracker = DebugActivationTracker()
        reviewPromptManager = ReviewPromptManager(preferences)
        dataExportManager = DataExportManager(
            context = context.applicationContext,
            database = database,
            preferences = preferences,
            reminderScheduler = reminderScheduler
        )
    }

    suspend fun seedDemoDataIfNeeded() {
        seeder.seedIfEmpty()
    }
}
