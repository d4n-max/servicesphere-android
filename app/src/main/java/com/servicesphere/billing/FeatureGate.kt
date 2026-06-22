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

enum class PaywallTrigger(
    val routeValue: String,
    val title: String,
    val body: String
) {
    GENERIC(
        "generic",
        "Run more of your service business with ServiceSphere Pro",
        "Upgrade when you're ready to manage clients, jobs, quotes, and invoices without limits."
    ),
    CLIENT_LIMIT(
        "client_limit",
        "You've reached your free client limit",
        "Free includes 5 clients so you can try ServiceSphere with real work. Upgrade to Pro to keep growing your customer list and job history."
    ),
    JOB_LIMIT(
        "job_limit",
        "You've organized 10 jobs with ServiceSphere",
        "Pro removes the job limit so your work history can keep growing with your business."
    ),
    QUOTE_LIMIT(
        "quote_limit",
        "You've reached your free quote limit",
        "Free includes 3 quotes so you can try ServiceSphere with real estimates. Upgrade to Pro for unlimited quotes."
    ),
    INVOICE_LIMIT(
        "invoice_limit",
        "You've reached your free invoice limit",
        "Free includes 3 invoices so you can try ServiceSphere with real billing. Upgrade to Pro for unlimited invoices."
    ),
    PHOTO_LIMIT(
        "photo_limit",
        "Keep a full photo history with Pro",
        "Free includes 3 photos per job. Upgrade to Pro to keep unlimited site photos and proof with each job."
    ),
    PDF_EXPORT(
        "pdf_export",
        "Export professional PDFs with Pro",
        "Send polished quotes and invoices with your business details, job info, and customer records."
    ),
    SIGNATURE(
        "signature",
        "Capture customer approval with Pro",
        "Add signatures to job records so completed work is easier to document."
    ),
    BRANDING(
        "branding",
        "Make every quote look like it came from your business",
        "Add your logo and business details with ServiceSphere Pro."
    ),
    ADVANCED_REPORTS(
        "advanced_reports",
        "Unlock advanced reports with Pro",
        "See more of the business numbers behind your clients, jobs, quotes, and invoices."
    );

    companion object {
        fun fromRouteValue(value: String?): PaywallTrigger =
            entries.firstOrNull { it.routeValue == value } ?: GENERIC
    }
}

data class FeatureGateResult(
    val allowed: Boolean,
    val title: String = "",
    val message: String = "",
    val featureName: String = "",
    val suggestedActionLabel: String = "Upgrade to Pro",
    val trigger: PaywallTrigger = PaywallTrigger.GENERIC
)

fun allowedGate() = FeatureGateResult(allowed = true)
