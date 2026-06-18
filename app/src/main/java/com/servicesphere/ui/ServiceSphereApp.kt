package com.servicesphere.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.servicesphere.data.ServiceLocator
import com.servicesphere.billing.FeatureGateResult
import com.servicesphere.ui.components.ServiceSphereBottomBar
import com.servicesphere.ui.components.ServiceSphereTopBar
import com.servicesphere.ui.navigation.Route
import com.servicesphere.ui.navigation.bottomDestinations
import com.servicesphere.ui.navigation.captureSignatureRoute
import com.servicesphere.ui.navigation.clientDetailRoute
import com.servicesphere.ui.navigation.composeMessageRoute
import com.servicesphere.ui.navigation.createJobRoute
import com.servicesphere.ui.navigation.createInvoiceRoute
import com.servicesphere.ui.navigation.createQuoteRoute
import com.servicesphere.ui.navigation.editClientRoute
import com.servicesphere.ui.navigation.editJobRoute
import com.servicesphere.ui.navigation.editInvoiceRoute
import com.servicesphere.ui.navigation.editQuoteRoute
import com.servicesphere.ui.navigation.invoiceDetailRoute
import com.servicesphere.ui.navigation.jobDetailRoute
import com.servicesphere.ui.navigation.paywallRoute
import com.servicesphere.ui.navigation.quoteDetailRoute
import com.servicesphere.ui.navigation.walkthroughRoute
import com.servicesphere.ui.components.PremiumGateDialog
import com.servicesphere.ui.screens.onboarding.BusinessSetupScreen
import com.servicesphere.ui.screens.clients.ClientDetailScreen
import com.servicesphere.ui.screens.clients.ClientFormScreen
import com.servicesphere.ui.screens.clients.ClientsScreen
import com.servicesphere.ui.screens.dashboard.DashboardScreen
import com.servicesphere.ui.screens.invoices.InvoiceDetailScreen
import com.servicesphere.ui.screens.invoices.InvoiceFormScreen
import com.servicesphere.ui.screens.invoices.InvoicesScreen
import com.servicesphere.ui.screens.jobs.JobDetailScreen
import com.servicesphere.ui.screens.jobs.JobFormScreen
import com.servicesphere.ui.screens.jobs.JobsScreen
import com.servicesphere.ui.screens.jobs.JobsViewMode
import com.servicesphere.ui.screens.messaging.MessageComposerScreen
import com.servicesphere.ui.screens.onboarding.OnboardingScreen
import com.servicesphere.ui.screens.onboarding.WalkthroughScreen
import com.servicesphere.ui.screens.paywall.PaywallScreen
import com.servicesphere.ui.screens.placeholder.QuickActionPlaceholderScreen
import com.servicesphere.ui.screens.quotes.QuoteDetailScreen
import com.servicesphere.ui.screens.quotes.QuoteFormScreen
import com.servicesphere.ui.screens.quotes.QuotesScreen
import com.servicesphere.ui.screens.signatures.SignatureCaptureScreen
import com.servicesphere.ui.screens.settings.BusinessProfileScreen
import com.servicesphere.ui.screens.settings.CurrencyTaxSettingsScreen
import com.servicesphere.ui.screens.settings.DataExportScreen
import com.servicesphere.ui.screens.settings.DocumentSettingsScreen
import com.servicesphere.ui.screens.settings.ReminderSettingsScreen
import com.servicesphere.ui.screens.settings.SettingsScreen
import com.servicesphere.ui.screens.splash.SplashScreen
import com.servicesphere.ui.theme.ServiceSphereTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ServiceSphereApp(initialJobId: String? = null) {
    ServiceSphereTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val currentRoute = currentDestination?.route
        val scope = rememberCoroutineScope()
        var blockedGate by remember { mutableStateOf<FeatureGateResult?>(null) }
        var handledInitialJobId by remember { mutableStateOf(false) }
        val showTopBar = currentRoute != Route.Onboarding.path &&
            currentRoute != Route.Splash.path &&
            currentRoute != Route.BusinessSetup.path &&
            currentRoute != Route.Walkthrough.path
        val showBottomBar = currentRoute in bottomDestinations.map { it.route.path }

        fun runGated(check: suspend () -> FeatureGateResult, action: () -> Unit) {
            scope.launch {
                val result = check()
                if (result.allowed) action() else blockedGate = result
            }
        }

        Scaffold(
            topBar = {
                if (showTopBar) {
                    ServiceSphereTopBar(title = if (currentRoute == Route.Dashboard.path) "ServiceSphere" else topBarTitle(currentRoute))
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    ServiceSphereBottomBar(
                        destinations = bottomDestinations,
                        currentRoute = currentRoute,
                        onDestinationClick = { destination ->
                            navController.navigate(destination.route.path) {
                                popUpTo(Route.Dashboard.path) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Route.Splash.path,
                modifier = Modifier.padding(padding)
            ) {
                composable(Route.Splash.path) {
                    LaunchedEffect(Unit) {
                        delay(700)
                        val destination = initialJobId?.takeIf { isBusinessSetupSatisfied() }?.let { jobDetailRoute(it) }
                            ?: resolveStartDestination()
                        navController.navigate(destination) {
                            popUpTo(Route.Splash.path) { inclusive = true }
                        }
                    }
                    SplashScreen()
                }
                composable(Route.Onboarding.path) {
                    OnboardingScreen(
                        onFinished = {
                            scope.launch {
                                val destination = if (isBusinessSetupSatisfied()) resolvePostSetupDestination() else Route.BusinessSetup.path
                                navController.navigate(destination) {
                                    popUpTo(Route.Onboarding.path) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(Route.BusinessSetup.path) {
                    BusinessSetupScreen(
                        onFinished = {
                            scope.launch {
                                navController.navigate(resolvePostSetupDestination()) {
                                    popUpTo(Route.BusinessSetup.path) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(
                    route = Route.Walkthrough.path,
                    arguments = listOf(navArgument("source") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { entry ->
                    val source = entry.arguments?.getString("source")
                    WalkthroughScreen(
                        onFinished = {
                            if (source == "settings") {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Route.Dashboard.path) {
                                    popUpTo(Route.Walkthrough.path) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable(Route.Dashboard.path) {
                    DashboardScreen(
                        onNewJob = { runGated(ServiceLocator.featureGateManager::canCreateJob) { navController.navigate(createJobRoute()) } },
                        onNewClient = { runGated(ServiceLocator.featureGateManager::canCreateClient) { navController.navigate(Route.CreateClient.path) } },
                        onNewQuote = { runGated(ServiceLocator.featureGateManager::canCreateQuote) { navController.navigate(createQuoteRoute()) } },
                        onNewInvoice = { runGated(ServiceLocator.featureGateManager::canCreateInvoice) { navController.navigate(createInvoiceRoute()) } },
                        onViewCalendar = { navController.navigate(Route.Calendar.path) }
                    )
                }
                composable(Route.Jobs.path) {
                    JobsScreen(
                        onAddJob = { runGated(ServiceLocator.featureGateManager::canCreateJob) { navController.navigate(createJobRoute()) } },
                        onJobClick = { jobId -> navController.navigate(jobDetailRoute(jobId)) }
                    )
                }
                composable(Route.Calendar.path) {
                    JobsScreen(
                        initialViewMode = JobsViewMode.CALENDAR,
                        onAddJob = { runGated(ServiceLocator.featureGateManager::canCreateJob) { navController.navigate(createJobRoute()) } },
                        onJobClick = { jobId -> navController.navigate(jobDetailRoute(jobId)) }
                    )
                }
                composable(Route.Clients.path) {
                    ClientsScreen(
                        onAddClient = { runGated(ServiceLocator.featureGateManager::canCreateClient) { navController.navigate(Route.CreateClient.path) } },
                        onClientClick = { clientId -> navController.navigate(clientDetailRoute(clientId)) }
                    )
                }
                composable(Route.Quotes.path) {
                    QuotesScreen(
                        onAddQuote = { runGated(ServiceLocator.featureGateManager::canCreateQuote) { navController.navigate(createQuoteRoute()) } },
                        onQuoteClick = { quoteId -> navController.navigate(quoteDetailRoute(quoteId)) }
                    )
                }
                composable(Route.Invoices.path) {
                    InvoicesScreen(
                        onAddInvoice = { runGated(ServiceLocator.featureGateManager::canCreateInvoice) { navController.navigate(createInvoiceRoute()) } },
                        onInvoiceClick = { invoiceId -> navController.navigate(invoiceDetailRoute(invoiceId)) },
                        onEditInvoice = { invoiceId -> navController.navigate(editInvoiceRoute(invoiceId)) }
                    )
                }
                composable(Route.Settings.path) {
                    SettingsScreen(
                        onBusinessProfile = { navController.navigate(Route.BusinessProfile.path) },
                        onBusinessSetup = { navController.navigate(Route.BusinessSetup.path) },
                        onCurrencyTax = { navController.navigate(Route.CurrencyTaxSettings.path) },
                        onDocumentSettings = { navController.navigate(Route.DocumentSettings.path) },
                        onReminderSettings = { navController.navigate(Route.ReminderSettings.path) },
                        onSubscription = { navController.navigate(paywallRoute("settings_subscription")) },
                        onDataPrivacy = { navController.navigate(Route.DataPrivacy.path) },
                        onReplayWalkthrough = { navController.navigate(walkthroughRoute("settings")) }
                    )
                }
                composable(
                    route = Route.Paywall.path,
                    arguments = listOf(navArgument("source") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { entry ->
                    PaywallScreen(
                        source = entry.arguments?.getString("source"),
                        onMaybeLater = { navController.popBackStack() }
                    )
                }
                composable(Route.BusinessProfile.path) {
                    BusinessProfileScreen(
                        onBack = { navController.popBackStack() },
                        onGateBlocked = { blockedGate = it }
                    )
                }
                composable(Route.CurrencyTaxSettings.path) {
                    CurrencyTaxSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Route.DocumentSettings.path) {
                    DocumentSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Route.ReminderSettings.path) {
                    ReminderSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Route.DataPrivacy.path) {
                    DataExportScreen(
                        onBack = { navController.popBackStack() },
                        onDeleteComplete = { resetSetup ->
                            navController.navigate(if (resetSetup) Route.Onboarding.path else Route.Dashboard.path) {
                                popUpTo(Route.Dashboard.path) { inclusive = !resetSetup }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = Route.ComposeMessage.path,
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("clientId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("jobId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("quoteId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("invoiceId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    MessageComposerScreen(
                        type = entry.arguments?.getString("type"),
                        clientId = entry.arguments?.getString("clientId"),
                        jobId = entry.arguments?.getString("jobId"),
                        quoteId = entry.arguments?.getString("quoteId"),
                        invoiceId = entry.arguments?.getString("invoiceId"),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Route.CreateJob.path,
                    arguments = listOf(navArgument("clientId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { entry ->
                    JobFormScreen(
                        jobId = null,
                        preselectedClientId = entry.arguments?.getString("clientId"),
                        onSaved = { jobId ->
                            navController.navigate(jobDetailRoute(jobId)) {
                                popUpTo(Route.CreateJob.path) { inclusive = true }
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Route.JobDetail.path,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { entry ->
                    val jobId = entry.arguments?.getString("jobId")
                    if (jobId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Job not found",
                            message = "This job route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        JobDetailScreen(
                            jobId = jobId,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate(editJobRoute(id)) },
                            onDeleted = {
                                navController.navigate(Route.Jobs.path) {
                                    popUpTo(Route.Jobs.path) { inclusive = true }
                                }
                            },
                            onCreateQuote = { runGated(ServiceLocator.featureGateManager::canCreateQuote) { navController.navigate(createQuoteRoute(jobId = jobId)) } },
                            onCreateInvoice = { runGated(ServiceLocator.featureGateManager::canCreateInvoice) { navController.navigate(createInvoiceRoute(jobId = jobId)) } },
                            onComposeMessage = { type -> navController.navigate(composeMessageRoute(type.routeValue, jobId = jobId)) },
                            onCaptureSignature = { runGated(ServiceLocator.featureGateManager::canCaptureSignature) { navController.navigate(captureSignatureRoute(jobId = jobId)) } },
                            onPhotoGateBlocked = { blockedGate = it }
                        )
                    }
                }
                composable(
                    route = Route.EditJob.path,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { entry ->
                    val jobId = entry.arguments?.getString("jobId")
                    if (jobId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Job not found",
                            message = "This edit route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        JobFormScreen(
                            jobId = jobId,
                            preselectedClientId = null,
                            onSaved = { savedId ->
                                navController.navigate(jobDetailRoute(savedId)) {
                                    popUpTo(editJobRoute(jobId)) { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
                composable(Route.CreateClient.path) {
                    ClientFormScreen(
                        clientId = null,
                        onSaved = { clientId ->
                            navController.navigate(clientDetailRoute(clientId)) {
                                popUpTo(Route.Clients.path)
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Route.ClientDetail.path,
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) { entry ->
                    val clientId = entry.arguments?.getString("clientId")
                    if (clientId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Client not found",
                            message = "This client route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        ClientDetailScreen(
                            clientId = clientId,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate(editClientRoute(id)) },
                            onDeleted = {
                                navController.navigate(Route.Clients.path) {
                                    popUpTo(Route.Clients.path) { inclusive = true }
                                }
                            },
                            onNewJob = { runGated(ServiceLocator.featureGateManager::canCreateJob) { navController.navigate(createJobRoute(clientId)) } },
                            onNewQuote = { runGated(ServiceLocator.featureGateManager::canCreateQuote) { navController.navigate(createQuoteRoute(clientId = clientId)) } },
                            onNewInvoice = { runGated(ServiceLocator.featureGateManager::canCreateInvoice) { navController.navigate(createInvoiceRoute(clientId = clientId)) } },
                            onComposeMessage = { type -> navController.navigate(composeMessageRoute(type.routeValue, clientId = clientId)) }
                        )
                    }
                }
                composable(
                    route = Route.EditClient.path,
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) { entry ->
                    val clientId = entry.arguments?.getString("clientId")
                    if (clientId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Client not found",
                            message = "This edit route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        ClientFormScreen(
                            clientId = clientId,
                            onSaved = { savedId ->
                                navController.navigate(clientDetailRoute(savedId)) {
                                    popUpTo(editClientRoute(clientId)) { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
                composable(
                    route = Route.CreateQuote.path,
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("jobId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    QuoteFormScreen(
                        quoteId = null,
                        preselectedClientId = entry.arguments?.getString("clientId"),
                        preselectedJobId = entry.arguments?.getString("jobId"),
                        onSaved = { quoteId ->
                            navController.navigate(quoteDetailRoute(quoteId)) {
                                popUpTo(Route.Quotes.path)
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Route.QuoteDetail.path,
                    arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
                ) { entry ->
                    val quoteId = entry.arguments?.getString("quoteId")
                    if (quoteId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Quote not found",
                            message = "This quote route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        QuoteDetailScreen(
                            quoteId = quoteId,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate(editQuoteRoute(id)) },
                            onDeleted = {
                                navController.navigate(Route.Quotes.path) {
                                    popUpTo(Route.Quotes.path) { inclusive = true }
                                }
                            },
                            onConvertedToInvoice = { invoiceId -> navController.navigate(invoiceDetailRoute(invoiceId)) },
                            onComposeMessage = { type -> navController.navigate(composeMessageRoute(type.routeValue, quoteId = quoteId)) },
                            onGateBlocked = { blockedGate = it }
                        )
                    }
                }
                composable(
                    route = Route.EditQuote.path,
                    arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
                ) { entry ->
                    val quoteId = entry.arguments?.getString("quoteId")
                    if (quoteId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Quote not found",
                            message = "This edit route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        QuoteFormScreen(
                            quoteId = quoteId,
                            preselectedClientId = null,
                            preselectedJobId = null,
                            onSaved = { savedId ->
                                navController.navigate(quoteDetailRoute(savedId)) {
                                    popUpTo(editQuoteRoute(quoteId)) { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
                composable(
                    route = Route.CreateInvoice.path,
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("jobId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("quoteId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    InvoiceFormScreen(
                        invoiceId = null,
                        preselectedClientId = entry.arguments?.getString("clientId"),
                        preselectedJobId = entry.arguments?.getString("jobId"),
                        preselectedQuoteId = entry.arguments?.getString("quoteId"),
                        onSaved = { invoiceId ->
                            navController.navigate(invoiceDetailRoute(invoiceId)) {
                                popUpTo(Route.Invoices.path)
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Route.InvoiceDetail.path,
                    arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
                ) { entry ->
                    val invoiceId = entry.arguments?.getString("invoiceId")
                    if (invoiceId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Invoice not found",
                            message = "This invoice route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        InvoiceDetailScreen(
                            invoiceId = invoiceId,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate(editInvoiceRoute(id)) },
                            onDeleted = {
                                navController.navigate(Route.Invoices.path) {
                                    popUpTo(Route.Invoices.path) { inclusive = true }
                                }
                            },
                            onComposeMessage = { type -> navController.navigate(composeMessageRoute(type.routeValue, invoiceId = invoiceId)) },
                            onCaptureSignature = { runGated(ServiceLocator.featureGateManager::canCaptureSignature) { navController.navigate(captureSignatureRoute(invoiceId = invoiceId)) } }
                        )
                    }
                }
                composable(
                    route = Route.EditInvoice.path,
                    arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
                ) { entry ->
                    val invoiceId = entry.arguments?.getString("invoiceId")
                    if (invoiceId.isNullOrBlank()) {
                        QuickActionPlaceholderScreen(
                            title = "Invoice not found",
                            message = "This edit route is missing an identifier.",
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        InvoiceFormScreen(
                            invoiceId = invoiceId,
                            preselectedClientId = null,
                            preselectedJobId = null,
                            preselectedQuoteId = null,
                            onSaved = { savedId ->
                                navController.navigate(invoiceDetailRoute(savedId)) {
                                    popUpTo(editInvoiceRoute(invoiceId)) { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
                composable(
                    route = Route.CaptureSignature.path,
                    arguments = listOf(
                        navArgument("jobId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("invoiceId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val jobId = entry.arguments?.getString("jobId")
                    val invoiceId = entry.arguments?.getString("invoiceId")
                    SignatureCaptureScreen(
                        jobId = jobId,
                        invoiceId = invoiceId,
                        onSaved = {
                            when {
                                !invoiceId.isNullOrBlank() -> navController.navigate(invoiceDetailRoute(invoiceId)) {
                                    popUpTo(Route.CaptureSignature.path) { inclusive = true }
                                }
                                !jobId.isNullOrBlank() -> navController.navigate(jobDetailRoute(jobId)) {
                                    popUpTo(Route.CaptureSignature.path) { inclusive = true }
                                }
                                else -> navController.popBackStack()
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
        }

        LaunchedEffect(initialJobId, currentRoute) {
            val jobId = initialJobId
            if (!handledInitialJobId && !jobId.isNullOrBlank() && currentRoute == Route.Dashboard.path && isBusinessSetupSatisfied()) {
                handledInitialJobId = true
                navController.navigate(jobDetailRoute(jobId))
            }
        }

        blockedGate?.let { gate ->
            PremiumGateDialog(
                gate = gate,
                onDismiss = { blockedGate = null },
                onUpgrade = {
                    blockedGate = null
                    navController.navigate(paywallRoute(gate.featureName))
                }
            )
        }
    }
}

private fun topBarTitle(route: String?): String = when (route) {
    Route.Dashboard.path -> "Dashboard"
    Route.BusinessSetup.path -> "Setup"
    Route.Walkthrough.path -> "Walkthrough"
    Route.Jobs.path -> "Jobs"
    Route.Calendar.path -> "Calendar"
    Route.Clients.path -> "Clients"
    Route.Quotes.path -> "Quotes"
    Route.Invoices.path -> "Invoices"
    Route.Settings.path -> "Settings"
    Route.BusinessProfile.path -> "Business Profile"
    Route.CurrencyTaxSettings.path -> "Currency & Tax"
    Route.DocumentSettings.path -> "Document Settings"
    Route.ReminderSettings.path -> "Reminders"
    Route.DataPrivacy.path -> "Data & Privacy"
    Route.ComposeMessage.path -> "Message Client"
    Route.Paywall.path -> "ServiceSphere Pro"
    Route.CreateJob.path -> "New Job"
    Route.JobDetail.path -> "Job Details"
    Route.EditJob.path -> "Edit Job"
    Route.CreateClient.path -> "New Client"
    Route.ClientDetail.path -> "Client Details"
    Route.EditClient.path -> "Edit Client"
    Route.CreateQuote.path -> "New Quote"
    Route.QuoteDetail.path -> "Quote Details"
    Route.EditQuote.path -> "Edit Quote"
    Route.CreateInvoice.path -> "New Invoice"
    Route.InvoiceDetail.path -> "Invoice Details"
    Route.EditInvoice.path -> "Edit Invoice"
    Route.CaptureSignature.path -> "Capture Signature"
    else -> "ServiceSphere"
}

private suspend fun resolveStartDestination(): String {
    val hasCompletedOnboarding = ServiceLocator.preferences.hasCompletedOnboarding.first()
    val hasCompletedBusinessSetup = isBusinessSetupSatisfied()
    return when {
        !hasCompletedOnboarding -> Route.Onboarding.path
        !hasCompletedBusinessSetup -> Route.BusinessSetup.path
        else -> {
            if (!ServiceLocator.preferences.hasSeenWalkthrough.first()) {
                ServiceLocator.preferences.setWalkthroughSeen(true)
            }
            Route.Dashboard.path
        }
    }
}

private suspend fun resolvePostSetupDestination(): String =
    if (ServiceLocator.preferences.hasSeenWalkthrough.first()) {
        Route.Dashboard.path
    } else {
        walkthroughRoute("setup")
    }

private suspend fun isBusinessSetupSatisfied(): Boolean {
    val preferenceComplete = ServiceLocator.preferences.hasCompletedBusinessSetup.first()
    if (preferenceComplete) return true
    val hasProfile = !ServiceLocator.businessRepository.getBusinessProfileOnce()?.businessName.isNullOrBlank()
    if (hasProfile) {
        ServiceLocator.preferences.setBusinessSetupComplete(true)
    }
    return hasProfile
}
