package com.servicesphere.billing

enum class ProFeature(val label: String) {
    CREATE_CLIENT("Create client"),
    CREATE_JOB("Create job"),
    CREATE_QUOTE("Create quote"),
    CREATE_INVOICE("Create invoice"),
    ADD_PHOTO_PROOF("Add photo proof"),
    CAPTURE_SIGNATURE("Capture signature"),
    USE_BUSINESS_LOGO_ON_PDF("Business logo on PDF"),
    REMOVE_PDF_WATERMARK("Remove PDF watermark"),
    PREMIUM_PDF_TEMPLATE("Premium PDF template"),
    EXPORT_UNLIMITED_DOCUMENTS("Export unlimited documents")
}

data class FeatureGateResult(
    val allowed: Boolean,
    val title: String = "",
    val message: String = "",
    val featureName: String = "",
    val suggestedActionLabel: String = "Upgrade to Pro"
)

fun allowedGate() = FeatureGateResult(allowed = true)
