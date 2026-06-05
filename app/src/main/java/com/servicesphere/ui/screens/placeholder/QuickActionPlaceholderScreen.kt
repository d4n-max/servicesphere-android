package com.servicesphere.ui.screens.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton

@Composable
fun QuickActionPlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader(title, "This flow is queued for the next implementation task.") }
        item {
            ServiceSphereCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(message)
                    ServiceSphereOutlinedButton(label = "Back", onClick = onBack)
                }
            }
        }
    }
}
