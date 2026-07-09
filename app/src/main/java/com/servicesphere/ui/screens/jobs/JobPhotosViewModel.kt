package com.servicesphere.ui.screens.jobs

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.data.local.JobPhotoEntity
import com.servicesphere.data.local.JobPhotoStorage
import com.servicesphere.data.repository.JobPhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class JobPhotoUiModel(
    val id: String,
    val jobId: String,
    val localUri: String,
    val caption: String?,
    val createdAt: Long
)

data class JobPhotosUiState(
    val isLoading: Boolean = true,
    val jobId: String = "",
    val photos: List<JobPhotoUiModel> = emptyList(),
    val selectedPhoto: JobPhotoUiModel? = null,
    val isAddingPhoto: Boolean = false,
    val errorMessage: String? = null,
    val captionEditPhotoId: String? = null,
    val captionDraft: String = "",
    val successMessage: String? = null
)

class JobPhotosViewModel(
    private val jobId: String,
    private val repository: JobPhotoRepository,
    private val storage: JobPhotoStorage,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val selectedPhotoId = MutableStateFlow<String?>(null)
    private val isAddingPhoto = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)
    private val captionEditPhotoId = MutableStateFlow<String?>(null)
    private val captionDraft = MutableStateFlow("")

    private val baseRows = combine(
        repository.observePhotosForJob(jobId),
        selectedPhotoId,
        isAddingPhoto,
        errorMessage
    ) { photos, selectedId, adding, error ->
        PhotoBaseRows(photos, selectedId, adding, error)
    }

    private val captionRows = combine(
        captionEditPhotoId,
        captionDraft,
        successMessage
    ) { editId, draft, success ->
        CaptionRows(editId, draft, success)
    }

    val uiState: StateFlow<JobPhotosUiState> = combine(baseRows, captionRows) { base, caption ->
        val models = base.photos.map { it.toUiModel() }
        JobPhotosUiState(
            isLoading = false,
            jobId = jobId,
            photos = models,
            selectedPhoto = models.firstOrNull { it.id == base.selectedPhotoId },
            isAddingPhoto = base.isAddingPhoto,
            errorMessage = base.errorMessage,
            captionEditPhotoId = caption.captionEditPhotoId,
            captionDraft = caption.captionDraft,
            successMessage = caption.successMessage
        )
    }
        .catch { error -> emit(JobPhotosUiState(isLoading = false, jobId = jobId, errorMessage = error.message ?: "Unable to load job photos")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobPhotosUiState(jobId = jobId))

    fun loadPhotos(jobId: String) {
        if (jobId != this.jobId) errorMessage.value = "This photo view is linked to a different job."
    }

    fun addPhotoFromUri(jobId: String, sourceUri: Uri?) {
        if (sourceUri == null) return
        addPhoto(jobId, sourceUri, "Photo added", "library")
    }

    fun addCapturedPhoto(jobId: String, capturedUri: Uri?) {
        if (capturedUri == null) return
        addPhoto(jobId, capturedUri, "Photo added", "camera")
    }

    fun createCameraImageUri(): Uri = storage.createCameraImageUri(jobId)

    fun onPhotoSelected(photoId: String) {
        selectedPhotoId.value = photoId
    }

    fun closePhotoPreview() {
        selectedPhotoId.value = null
    }

    fun startEditCaption(photoId: String) {
        val photo = uiState.value.photos.firstOrNull { it.id == photoId }
        captionEditPhotoId.value = photoId
        captionDraft.value = photo?.caption.orEmpty()
    }

    fun onCaptionChanged(value: String) {
        captionDraft.value = value
    }

    fun saveCaption() {
        val photoId = captionEditPhotoId.value ?: return
        viewModelScope.launch {
            runCatching {
                val photo = repository.getPhotoByIdOnce(photoId) ?: return@runCatching
                repository.updatePhoto(photo.copy(caption = captionDraft.value.trim().ifBlank { null }))
            }.onSuccess {
                captionEditPhotoId.value = null
                captionDraft.value = ""
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Could not save caption"
            }
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            runCatching {
                val photo = repository.getPhotoByIdOnce(photoId) ?: return@runCatching
                repository.deletePhoto(photo)
                storage.deleteStoredPhoto(photo.localUri)
            }.onSuccess {
                if (selectedPhotoId.value == photoId) selectedPhotoId.value = null
                successMessage.value = "Photo deleted"
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Could not delete photo"
            }
        }
    }

    fun clearError() = errorMessage.update { null }
    fun clearSuccess() = successMessage.update { null }
    fun cancelCaptionEdit() {
        captionEditPhotoId.value = null
        captionDraft.value = ""
    }

    private fun addPhoto(targetJobId: String, sourceUri: Uri, success: String, source: String) {
        viewModelScope.launch {
            isAddingPhoto.value = true
            runCatching {
                val localUri = storage.copyImageToAppStorage(targetJobId, sourceUri)
                repository.insertPhoto(
                    JobPhotoEntity(
                        id = UUID.randomUUID().toString(),
                        jobId = targetJobId,
                        localUri = localUri,
                        caption = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                activationTracker.trackFirst(
                    ActivationEvents.FIRST_PHOTO_PROOF_ADDED,
                    mapOf(
                        ActivationParams.SOURCE_SCREEN to "job_photos",
                        ActivationParams.PHOTO_SOURCE to source
                    )
                )
                successMessage.value = success
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Couldn't add photo"
            }
            isAddingPhoto.value = false
        }
    }

    class Factory(
        private val jobId: String,
        private val repository: JobPhotoRepository,
        private val storage: JobPhotoStorage,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            JobPhotosViewModel(jobId, repository, storage, activationTracker) as T
    }
}

private fun JobPhotoEntity.toUiModel(): JobPhotoUiModel = JobPhotoUiModel(
    id = id,
    jobId = jobId,
    localUri = localUri,
    caption = caption,
    createdAt = createdAt
)

private data class PhotoBaseRows(
    val photos: List<JobPhotoEntity>,
    val selectedPhotoId: String?,
    val isAddingPhoto: Boolean,
    val errorMessage: String?
)

private data class CaptionRows(
    val captionEditPhotoId: String?,
    val captionDraft: String,
    val successMessage: String?
)
