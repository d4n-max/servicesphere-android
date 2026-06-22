package com.servicesphere.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereLogo
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onCreateFirstJob: () -> Unit,
    onExploreSampleJob: () -> Unit
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        ServiceLocator.activationTracker.track(ActivationEvents.ONBOARDING_STARTED)
    }
    fun markOnboardingCompleteThen(action: () -> Unit) {
        scope.launch {
            ServiceLocator.preferences.setOnboardingComplete(true)
            action()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(horizontal = 24.dp, vertical = 28.dp)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ServiceSphereLogo(Modifier.size(64.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Run your service jobs without losing the details.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Keep clients, quotes, job notes, photos, signatures, routes, and invoices organized from the first call to payment.",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = ServiceSphereTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ServiceSphereButton("Create my first job") {
                ServiceLocator.activationTracker.track(ActivationEvents.FIRST_JOB_STARTED)
                markOnboardingCompleteThen(onCreateFirstJob)
            }
            ServiceSphereOutlinedButton("Explore with a sample job", Modifier.fillMaxWidth()) {
                ServiceLocator.activationTracker.track(ActivationEvents.ONBOARDING_DEMO_STARTED)
                markOnboardingCompleteThen(onExploreSampleJob)
            }
            Text(
                "Takes about 1 minute. You can change everything later.",
                color = ServiceSphereTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
