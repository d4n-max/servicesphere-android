package com.servicesphere.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.preferences.UserPreferences
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.domain.model.LineItemParentType
import com.servicesphere.domain.model.QuoteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class FirstJobUiState(
    val clientName: String = "",
    val jobTitle: String = "",
    val address: String = "",
    val dateText: String = LocalDate.now().toString(),
    val estimatedPrice: String = "",
    val notes: String = "",
    val clientNameError: String? = null,
    val jobTitleError: String? = null,
    val priceError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val createdJobId: String? = null,
    val createdSampleJobId: String? = null
)

class FirstJobViewModel(
    private val clientRepository: ClientRepository,
    private val jobRepository: JobRepository,
    private val quoteRepository: QuoteRepository,
    private val lineItemRepository: LineItemRepository,
    private val preferences: UserPreferences,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(FirstJobUiState())
    val uiState: StateFlow<FirstJobUiState> = _uiState.asStateFlow()
    private val zoneId = ZoneId.systemDefault()

    fun onClientNameChanged(value: String) = _uiState.update { it.copy(clientName = value, clientNameError = null) }
    fun onJobTitleChanged(value: String) = _uiState.update { it.copy(jobTitle = value, jobTitleError = null) }
    fun onAddressChanged(value: String) = _uiState.update { it.copy(address = value) }
    fun onDateChanged(value: String) = _uiState.update { it.copy(dateText = value, dateError = null) }
    fun onEstimatedPriceChanged(value: String) = _uiState.update { it.copy(estimatedPrice = value, priceError = null) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }
    fun resetCreatedJob() = _uiState.update { it.copy(createdJobId = null, createdSampleJobId = null) }

    fun saveFirstJob() {
        val current = _uiState.value
        val clientError = if (current.clientName.trim().isBlank()) "Client name is required" else null
        val jobError = if (current.jobTitle.trim().isBlank()) "Job title is required" else null
        val price = current.estimatedPrice.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val priceError = when {
            current.estimatedPrice.trim().isBlank() -> null
            price == null || price < 0.0 -> "Enter a valid price"
            else -> null
        }
        val scheduledAt = current.dateText.trim().takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli() }
                .getOrNull()
        }
        val dateError = if (current.dateText.isNotBlank() && scheduledAt == null) "Use date YYYY-MM-DD" else null
        if (clientError != null || jobError != null || priceError != null || dateError != null) {
            _uiState.update {
                it.copy(
                    clientNameError = clientError,
                    jobTitleError = jobError,
                    priceError = priceError,
                    dateError = dateError
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val client = ClientEntity(
                    name = current.clientName.trim(),
                    address = current.address.trim().ifBlank { null },
                    createdAt = now,
                    updatedAt = now
                )
                clientRepository.insertClient(client)
                activationTracker.track(ActivationEvents.FIRST_CLIENT_CREATED)
                val job = JobEntity(
                    clientId = client.id,
                    title = current.jobTitle.trim(),
                    address = current.address.trim().ifBlank { null },
                    scheduledAt = scheduledAt,
                    status = JobStatus.NEW,
                    estimatedPrice = price,
                    internalNotes = current.notes.trim().ifBlank { null },
                    createdAt = now,
                    updatedAt = now
                )
                jobRepository.insertJob(job)
                preferences.setOnboardingComplete(true)
                preferences.setFirstRealJobCreated(true)
                activationTracker.track(ActivationEvents.FIRST_JOB_CREATED)
                if (!job.internalNotes.isNullOrBlank() || !job.address.isNullOrBlank() || job.estimatedPrice != null) {
                    activationTracker.track(ActivationEvents.ACTIVATION_FIRST_JOB_ORGANIZED)
                }
                job.id
            }.onSuccess { jobId ->
                _uiState.update { it.copy(isSaving = false, createdJobId = jobId) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't create first job") }
            }
        }
    }

    fun createSampleJob() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val existing = preferences.sampleJobId.valueOrNull()
                    ?.let { jobRepository.getJobByIdOnce(it) }
                if (existing != null) return@runCatching existing.id
                val now = System.currentTimeMillis()
                val client = ClientEntity(
                    name = "Sarah Miller",
                    address = "1248 Maple Street",
                    notes = "Sample client for exploring ServiceSphere.",
                    createdAt = now,
                    updatedAt = now
                )
                clientRepository.insertClient(client)
                val job = JobEntity(
                    clientId = client.id,
                    title = "Repair leaking kitchen sink",
                    description = "Customer reports leak under sink.",
                    address = "1248 Maple Street",
                    scheduledAt = LocalDate.now().atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    status = JobStatus.NEW,
                    estimatedPrice = 185.0,
                    internalNotes = "Customer reports leak under sink.",
                    createdAt = now,
                    updatedAt = now
                )
                jobRepository.insertJob(job)
                val quote = QuoteEntity(
                    id = UUID.randomUUID().toString(),
                    clientId = client.id,
                    jobId = job.id,
                    quoteNumber = "Q-SAMPLE",
                    status = QuoteStatus.DRAFT,
                    issueDate = now,
                    subtotal = 185.0,
                    taxAmount = 0.0,
                    total = 185.0,
                    notes = "Sample quote for labor and parts.",
                    createdAt = now,
                    updatedAt = now
                )
                quoteRepository.insertQuote(quote)
                lineItemRepository.insertLineItems(
                    listOf(
                        LineItemEntity(parentId = quote.id, parentType = LineItemParentType.QUOTE, description = "Labor", quantity = 2.0, unitPrice = 75.0, total = 150.0),
                        LineItemEntity(parentId = quote.id, parentType = LineItemParentType.QUOTE, description = "Parts", quantity = 1.0, unitPrice = 35.0, total = 35.0)
                    )
                )
                preferences.setOnboardingComplete(true)
                preferences.setSampleJobId(job.id)
                activationTracker.track(ActivationEvents.ONBOARDING_DEMO_STARTED)
                job.id
            }.onSuccess { jobId ->
                _uiState.update { it.copy(isSaving = false, createdSampleJobId = jobId) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't create sample job") }
            }
        }
    }

    private suspend fun Flow<String?>.valueOrNull(): String? = first()

    class Factory(
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository,
        private val lineItemRepository: LineItemRepository,
        private val preferences: UserPreferences,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FirstJobViewModel(clientRepository, jobRepository, quoteRepository, lineItemRepository, preferences, activationTracker) as T
    }
}
