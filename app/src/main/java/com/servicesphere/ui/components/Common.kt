package com.servicesphere.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.servicesphere.R
import com.servicesphere.ui.navigation.BottomDestination
import com.servicesphere.ui.theme.ServiceSphereBackground
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereDarkSurface
import com.servicesphere.ui.theme.ServiceSphereOutline
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereSecondary
import com.servicesphere.ui.theme.ServiceSphereSecondaryContainer
import com.servicesphere.ui.theme.ServiceSphereSuccess
import com.servicesphere.ui.theme.ServiceSphereSurface
import com.servicesphere.ui.theme.ServiceSphereSurfaceLow
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import com.servicesphere.ui.theme.ServiceSphereWarning

@Composable
fun ServiceSphereLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ServiceSpherePrimary),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.servicesphere_icon),
            contentDescription = "ServiceSphere",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(48.dp).aspectRatio(1f)
        )
    }
}

@Composable
fun ServiceSphereButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ServiceSpherePrimary)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ServiceSphereOutlinedButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ServiceSphereTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(label, fontWeight = FontWeight.SemiBold, color = ServiceSpherePrimary)
    }
}

@Composable
fun ServiceSphereCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val cardModifier = modifier
        .fillMaxWidth()
        .border(1.dp, ServiceSphereOutline, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = ServiceSphereSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row {
            if (accentColor != null) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(116.dp)
                        .background(accentColor)
                )
            }
            Box(Modifier.padding(16.dp).weight(1f)) { content() }
        }
    }
}

@Composable
fun ServiceSphereMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badge: String? = null,
    tone: StatusTone = StatusTone.Neutral
) {
    ServiceSphereCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) Icon(icon, null, modifier = Modifier.size(18.dp), tint = toneColor(tone))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = ServiceSphereTextSecondary)
                }
                if (badge != null) StatusChip(badge, tone)
            }
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ServiceSphereMetricCard(label = label, value = value, modifier = modifier)
}

@Composable
fun StatusChip(label: String, tone: StatusTone = StatusTone.Neutral) {
    val color = toneColor(tone)
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

enum class StatusTone { Neutral, Info, Success, Warning, Danger, Primary }

fun toneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.Success -> ServiceSphereSuccess
    StatusTone.Warning -> ServiceSphereWarning
    StatusTone.Danger -> ServiceSphereDanger
    StatusTone.Info -> ServiceSphereSecondary
    StatusTone.Primary -> ServiceSpherePrimary
    StatusTone.Neutral -> ServiceSphereTextSecondary
}

@Composable
fun EmptyState(title: String, message: String, icon: ImageVector? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    ServiceSphereCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon == null) {
                ServiceSphereLogo(Modifier.size(56.dp))
            } else {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ServiceSphereSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = ServiceSpherePrimary)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ServiceSphereTextSecondary, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) ServiceSphereButton(actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) ServiceSphereTextButton(action, onAction)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = ServiceSphereTextSecondary)
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (primary) ServiceSpherePrimary else ServiceSphereSurfaceLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (primary) Color.White else ServiceSphereDarkSurface)
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = ServiceSphereDarkSurface)
    }
}

@Composable
fun ServiceSphereTopBar(title: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = ServiceSphereSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = ServiceSphereDarkSurface)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = ServiceSpherePrimary, fontWeight = FontWeight.Bold)
            ServiceSphereLogo(Modifier.size(36.dp))
        }
    }
}

@Composable
fun ServiceSphereBottomBar(
    destinations: List<BottomDestination>,
    currentRoute: String?,
    onDestinationClick: (BottomDestination) -> Unit
) {
    Surface(color = ServiceSphereSurface, tonalElevation = 4.dp, shadowElevation = 8.dp) {
        NavigationBar(containerColor = ServiceSphereSurface) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route.path
                NavigationBarItem(
                    selected = selected,
                    onClick = { onDestinationClick(destination) },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = {
                        Text(
                            destination.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceSphereTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ServiceSpherePrimary,
            unfocusedBorderColor = ServiceSphereOutline,
            focusedContainerColor = ServiceSphereSurface,
            unfocusedContainerColor = ServiceSphereSurface
        )
    )
}

@Composable
fun ServiceSphereSearchBar(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    ServiceSphereTextField(value = value, onValueChange = onValueChange, label = placeholder, modifier = modifier)
}

@Composable
fun ClientCard(name: String, detail: String, phone: String = "(555) 284-9102", status: String = "Job Complete", tone: StatusTone = StatusTone.Primary) {
    ServiceSphereCard(accentColor = toneColor(tone)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                InitialsCircle(name)
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = ServiceSphereTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(phone, color = ServiceSphereTextSecondary, style = MaterialTheme.typography.bodyMedium)
                StatusChip(status, tone)
            }
        }
    }
}

@Composable
fun JobCard(title: String, client: String, status: String, schedule: String?, price: String?, tone: StatusTone = StatusTone.Info) {
    ServiceSphereCard(accentColor = toneColor(tone)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(client, style = MaterialTheme.typography.bodyMedium, color = ServiceSphereTextSecondary)
                }
                StatusChip(status, tone)
            }
            Text(schedule ?: "No schedule", color = ServiceSphereTextSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Estimated", color = ServiceSphereTextSecondary, style = MaterialTheme.typography.labelMedium)
                Text(price ?: "Not set", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun InvoiceCard(number: String, client: String, status: String, dueDate: String?, total: String, tone: StatusTone = StatusTone.Warning) {
    ServiceSphereCard(accentColor = toneColor(tone)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InitialsCircle(client)
                    Column {
                        Text(client, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(number, color = ServiceSphereTextSecondary)
                    }
                }
                StatusChip(status, tone)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Due Date", color = ServiceSphereTextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text(dueDate ?: "Not set", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", color = ServiceSphereTextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text(total, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuoteCard(number: String, client: String, status: String, total: String) {
    ServiceSphereCard(accentColor = ServiceSphereSecondary) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(client, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(number, color = ServiceSphereTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status, StatusTone.Info)
                Text(total, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaywallBenefitRow(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ServiceSpherePrimary)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = ServiceSphereTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String? = null, danger: Boolean = false, onClick: (() -> Unit)? = null) {
    ServiceSphereCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (danger) ServiceSphereDanger else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ServiceSphereTextSecondary)
        }
    }
}

@Composable
private fun InitialsCircle(name: String) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
    Box(Modifier.size(48.dp).clip(CircleShape).background(ServiceSphereSurfaceLow), contentAlignment = Alignment.Center) {
        Text(initials.ifBlank { "SS" }, style = MaterialTheme.typography.titleMedium, color = ServiceSphereTextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GradientBrandBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .background(Brush.linearGradient(listOf(ServiceSpherePrimary, ServiceSphereSecondary)))
            .fillMaxWidth()
    ) { content() }
}
