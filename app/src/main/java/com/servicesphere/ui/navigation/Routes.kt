package com.servicesphere.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Onboarding : Route("onboarding")
    data object BusinessSetup : Route("business_setup")
    data object Walkthrough : Route("walkthrough?source={source}")
    data object Dashboard : Route("dashboard")
    data object Jobs : Route("jobs")
    data object Calendar : Route("calendar")
    data object Clients : Route("clients")
    data object Quotes : Route("quotes")
    data object Invoices : Route("invoices")
    data object Settings : Route("settings")
    data object BusinessProfile : Route("business_profile")
    data object CurrencyTaxSettings : Route("currency_tax_settings")
    data object DocumentSettings : Route("document_settings")
    data object ReminderSettings : Route("reminder_settings")
    data object DataPrivacy : Route("data_privacy")
    data object ComposeMessage : Route("compose_message?type={type}&clientId={clientId}&jobId={jobId}&quoteId={quoteId}&invoiceId={invoiceId}")
    data object Paywall : Route("paywall?source={source}")
    data object CreateJob : Route("create_job?clientId={clientId}")
    data object JobDetail : Route("job_detail/{jobId}")
    data object EditJob : Route("edit_job/{jobId}")
    data object CreateClient : Route("create_client")
    data object ClientDetail : Route("client_detail/{clientId}")
    data object EditClient : Route("edit_client/{clientId}")
    data object CreateQuote : Route("create_quote?clientId={clientId}&jobId={jobId}")
    data object QuoteDetail : Route("quote_detail/{quoteId}")
    data object EditQuote : Route("edit_quote/{quoteId}")
    data object CreateInvoice : Route("create_invoice?clientId={clientId}&jobId={jobId}&quoteId={quoteId}")
    data object InvoiceDetail : Route("invoice_detail/{invoiceId}")
    data object EditInvoice : Route("edit_invoice/{invoiceId}")
    data object CaptureSignature : Route("capture_signature?jobId={jobId}&invoiceId={invoiceId}")
}

fun clientDetailRoute(clientId: String): String = "client_detail/$clientId"

fun editClientRoute(clientId: String): String = "edit_client/$clientId"

fun createJobRoute(clientId: String? = null): String =
    if (clientId.isNullOrBlank()) "create_job" else "create_job?clientId=$clientId"

fun jobDetailRoute(jobId: String): String = "job_detail/$jobId"

fun editJobRoute(jobId: String): String = "edit_job/$jobId"

fun createQuoteRoute(clientId: String? = null, jobId: String? = null): String {
    val params = listOfNotNull(
        clientId?.takeIf { it.isNotBlank() }?.let { "clientId=$it" },
        jobId?.takeIf { it.isNotBlank() }?.let { "jobId=$it" }
    )
    return if (params.isEmpty()) "create_quote" else "create_quote?${params.joinToString("&")}"
}

fun quoteDetailRoute(quoteId: String): String = "quote_detail/$quoteId"

fun editQuoteRoute(quoteId: String): String = "edit_quote/$quoteId"

fun createInvoiceRoute(clientId: String? = null, jobId: String? = null, quoteId: String? = null): String {
    val params = listOfNotNull(
        clientId?.takeIf { it.isNotBlank() }?.let { "clientId=$it" },
        jobId?.takeIf { it.isNotBlank() }?.let { "jobId=$it" },
        quoteId?.takeIf { it.isNotBlank() }?.let { "quoteId=$it" }
    )
    return if (params.isEmpty()) "create_invoice" else "create_invoice?${params.joinToString("&")}"
}

fun invoiceDetailRoute(invoiceId: String): String = "invoice_detail/$invoiceId"

fun editInvoiceRoute(invoiceId: String): String = "edit_invoice/$invoiceId"

fun captureSignatureRoute(jobId: String? = null, invoiceId: String? = null): String {
    val params = listOfNotNull(
        jobId?.takeIf { it.isNotBlank() }?.let { "jobId=$it" },
        invoiceId?.takeIf { it.isNotBlank() }?.let { "invoiceId=$it" }
    )
    return if (params.isEmpty()) "capture_signature" else "capture_signature?${params.joinToString("&")}"
}

fun walkthroughRoute(source: String? = null): String =
    if (source.isNullOrBlank()) "walkthrough" else "walkthrough?source=$source"

fun composeMessageRoute(
    type: String,
    clientId: String? = null,
    jobId: String? = null,
    quoteId: String? = null,
    invoiceId: String? = null
): String {
    val params = listOfNotNull(
        "type=$type",
        clientId?.takeIf { it.isNotBlank() }?.let { "clientId=$it" },
        jobId?.takeIf { it.isNotBlank() }?.let { "jobId=$it" },
        quoteId?.takeIf { it.isNotBlank() }?.let { "quoteId=$it" },
        invoiceId?.takeIf { it.isNotBlank() }?.let { "invoiceId=$it" }
    )
    return "compose_message?${params.joinToString("&")}"
}

fun paywallRoute(source: String? = null): String =
    if (source.isNullOrBlank()) "paywall" else "paywall?source=$source"

data class BottomDestination(
    val route: Route,
    val label: String,
    val icon: ImageVector
)

val bottomDestinations = listOf(
    BottomDestination(Route.Dashboard, "Dashboard", Icons.Filled.Dashboard),
    BottomDestination(Route.Jobs, "Jobs", Icons.Filled.HomeRepairService),
    BottomDestination(Route.Clients, "Clients", Icons.Filled.Groups),
    BottomDestination(Route.Quotes, "Quotes", Icons.Filled.Description),
    BottomDestination(Route.Invoices, "Invoices", Icons.AutoMirrored.Filled.ReceiptLong),
    BottomDestination(Route.Settings, "Settings", Icons.Filled.Settings)
)
