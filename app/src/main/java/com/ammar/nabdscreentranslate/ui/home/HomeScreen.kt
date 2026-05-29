package com.ammar.nabdscreentranslate.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammar.nabdscreentranslate.translate.LanguageMapper
import com.ammar.nabdscreentranslate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleFloating: () -> Unit,
    onSourceLangChanged: (String) -> Unit,
    onTargetLangChanged: (String) -> Unit,
    onDownloadModels: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ─── Hero Section ────────────────────────────────────────
            Text(
                text = "ترجمة الشاشة الفورية",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "التقط النص من أي تطبيق وترجمه فورًا على جهازك",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ─── Main Action Button ──────────────────────────────────
            MainActionButton(
                isActive = uiState.isFloatingActive,
                onClick = {
                    if (!uiState.hasOverlayPermission) {
                        onRequestOverlayPermission()
                    } else {
                        onToggleFloating()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (uiState.isFloatingActive) "إيقاف الزر العائم" else "تشغيل الزر العائم",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = if (uiState.isFloatingActive) Success400 else TextMuted
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ─── Status Card ─────────────────────────────────────────
            StatusCard(isActive = uiState.isFloatingActive)

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Language Selection ──────────────────────────────────
            LensCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات اللغة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextLight
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LanguageDropdown(
                        label = "لغة المصدر",
                        selectedCode = uiState.sourceLang,
                        languages = LanguageMapper.supportedLanguages,
                        onSelected = onSourceLangChanged
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LanguageDropdown(
                        label = "لغة الهدف",
                        selectedCode = uiState.targetLang,
                        languages = LanguageMapper.targetLanguages,
                        onSelected = onTargetLangChanged
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Quick Actions ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CloudDownload,
                    label = "تحميل النموذج",
                    isLoading = uiState.isDownloadingModel,
                    statusText = uiState.downloadMessage,
                    onClick = onDownloadModels
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.History,
                    label = "السجل",
                    onClick = onNavigateToHistory
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Settings,
                    label = "الإعدادات",
                    onClick = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Privacy Badge ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Glass800.copy(alpha = 0.6f))
                    .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = "خصوصية",
                    tint = Success400,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "لا يتم حفظ صور الشاشة ولا إرسالها لأي خادم",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Components ──────────────────────────────────────────────────────────────

@Composable
private fun MainActionButton(isActive: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) Success400 else Cyan400,
        label = "border"
    )

    Box(
        modifier = Modifier
            .size(130.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Button
        Button(
            onClick = onClick,
            modifier = Modifier.size(110.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Success400.copy(alpha = 0.15f) else Cyan400.copy(alpha = 0.12f)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.4f)))
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.Stop else Icons.Filled.Translate,
                contentDescription = if (isActive) "إيقاف" else "تشغيل",
                modifier = Modifier.size(40.dp),
                tint = borderColor
            )
        }
    }
}

@Composable
private fun StatusCard(isActive: Boolean) {
    LensCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Success400 else TextDim)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isActive) "الزر العائم نشط" else "الزر العائم متوقف",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isActive) Success400 else TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isActive) "يعمل" else "معطّل",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }
}

@Composable
fun LensCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Glass800.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isLoading: Boolean = false,
    statusText: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Glass800.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isLoading) Amber400 else Cyan400,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextLight,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (statusText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (statusText.contains("✓")) Success400 else Amber400,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    label: String,
    selectedCode: String,
    languages: List<LanguageMapper.LanguageInfo>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLang = languages.find { it.code == selectedCode }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLang?.nativeName ?: selectedCode,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextDim) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan400,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = Glass700.copy(alpha = 0.3f),
                unfocusedContainerColor = Glass700.copy(alpha = 0.2f),
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextLight
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row {
                            Text(lang.nativeName, modifier = Modifier.weight(1f))
                            Text(
                                lang.displayName,
                                color = TextDim,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onSelected(lang.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
