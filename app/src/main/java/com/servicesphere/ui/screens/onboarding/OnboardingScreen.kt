package com.servicesphere.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereLogo
import com.servicesphere.ui.components.ServiceSphereTextButton
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereSecondaryContainer
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch

private data class OnboardingSlide(val title: String, val body: String, val icon: ImageVector)

private val slides = listOf(
    OnboardingSlide(
        "Run your service business from your phone",
        "Manage jobs, clients, quotes, invoices, photo proof, and signatures in one place.",
        Icons.Filled.AssignmentTurnedIn
    ),
    OnboardingSlide(
        "Send professional quotes and invoices",
        "Create branded PDFs and share them with clients in minutes.",
        Icons.AutoMirrored.Filled.ReceiptLong
    ),
    OnboardingSlide(
        "Capture proof and approval",
        "Attach job photos and collect client signatures before you leave the site.",
        Icons.Filled.CameraAlt
    ),
    OnboardingSlide(
        "Works offline in the field",
        "Keep your records available even when the job site has poor signal.",
        Icons.Filled.SignalCellularConnectedNoInternet4Bar
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val finish: () -> Unit = {
        scope.launch {
            ServiceLocator.preferences.setOnboardingComplete(true)
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(horizontal = 24.dp, vertical = 28.dp)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ServiceSphereLogo(Modifier.size(44.dp))
            ServiceSphereTextButton("Skip", onClick = finish)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ServiceSphereSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(slide.icon, contentDescription = null, tint = ServiceSpherePrimary, modifier = Modifier.size(84.dp))
                }
                Column(Modifier.padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(slide.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(slide.body, style = MaterialTheme.typography.bodyLarge, color = ServiceSphereTextSecondary, textAlign = TextAlign.Center)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(slides.size) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (pagerState.currentPage == index) 28.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (pagerState.currentPage == index) ServiceSpherePrimary else ServiceSphereTextSecondary.copy(alpha = 0.18f))
                )
            }
        }
        Column(Modifier.padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val isLastPage = pagerState.currentPage == slides.lastIndex
            ServiceSphereButton(
                if (isLastPage) "Get Started" else "Continue",
                onClick = {
                    if (isLastPage) {
                        finish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }
            )
            Text("Your complete field service ecosystem", color = ServiceSphereTextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
