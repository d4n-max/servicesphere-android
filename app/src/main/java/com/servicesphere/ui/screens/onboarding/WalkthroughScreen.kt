package com.servicesphere.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextButton
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereSecondary
import com.servicesphere.ui.theme.ServiceSphereSecondaryContainer
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch

private data class WalkthroughStep(
    val title: String,
    val body: String,
    val icon: ImageVector
)

private val walkthroughSteps = listOf(
    WalkthroughStep(
        title = "Welcome to ServiceSphere",
        body = "Manage clients, jobs, quotes, invoices, photo proof, and signatures from one offline-first workspace.",
        icon = Icons.Filled.AssignmentTurnedIn
    ),
    WalkthroughStep(
        title = "Start with clients",
        body = "Create client records with contact details, notes, and service history.",
        icon = Icons.Filled.Groups
    ),
    WalkthroughStep(
        title = "Plan every job",
        body = "Schedule work, track status, add reminders, and keep field notes organized.",
        icon = Icons.Filled.HomeRepairService
    ),
    WalkthroughStep(
        title = "Create quotes and invoices",
        body = "Build professional quotes, convert accepted work into invoices, and share PDFs.",
        icon = Icons.AutoMirrored.Filled.ReceiptLong
    ),
    WalkthroughStep(
        title = "Capture proof and approval",
        body = "Attach job photos and collect client signatures for a cleaner service record.",
        icon = Icons.Filled.CameraAlt
    ),
    WalkthroughStep(
        title = "You're ready to work",
        body = "Use the dashboard quick actions to create your first client, job, quote, or invoice.",
        icon = Icons.Filled.CheckCircle
    )
)

@Composable
fun WalkthroughScreen(onFinished: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun finishWalkthrough() {
        scope.launch {
            ServiceLocator.preferences.setWalkthroughSeen(true)
            onFinished()
        }
    }

    BackHandler {
        if (currentStep > 0) {
            currentStep -= 1
        } else {
            finishWalkthrough()
        }
    }

    val step = walkthroughSteps[currentStep]
    val isLastStep = currentStep == walkthroughSteps.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(PaddingValues(horizontal = 24.dp, vertical = 24.dp)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${currentStep + 1} of ${walkthroughSteps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = ServiceSphereTextSecondary
            )
            ServiceSphereTextButton("Skip", onClick = ::finishWalkthrough)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(ServiceSpherePrimary, ServiceSphereSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(step.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }
            Spacer(Modifier.height(34.dp))
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = step.body,
                style = MaterialTheme.typography.bodyLarge,
                color = ServiceSphereTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(walkthroughSteps.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = if (index == currentStep) 30.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (index == currentStep) ServiceSpherePrimary else ServiceSphereSecondaryContainer)
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceSphereButton(
                label = if (isLastStep) "Go to Dashboard" else "Next",
                onClick = {
                    if (isLastStep) finishWalkthrough() else currentStep += 1
                }
            )
            if (currentStep > 0) {
                ServiceSphereOutlinedButton(
                    label = "Back",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { currentStep -= 1 }
                )
            }
        }
    }
}
