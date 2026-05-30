package com.ammar.nabdscreentranslate.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.blur
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink900)
        ) {
            // ─── Atmospheric ember glow backdrop ─────────────────────
            AmbientGlow(isActive = uiState.isFloatingActive)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // ─── Brand lockup ────────────────────────────────────
                BrandHeader(onSettings = onNavigateToSettings)

                Spacer(modifier = Modifier.height(40.dp))

                // ─── Hero ────────────────────────────────────────────
                Text(
                    text = "ترجمة الشاشة الفورية",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "التقط أي نص على شاشتك وترجمه فورًا — تتم المعالجة على جهازك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ─── Main pulse action ───────────────────────────────
                MainActionButton(
                    isActive = uiState.isFloatingActive,
                    onClick = {
                        if (!uiState.hasOverlayPermission) onRequestOverlayPermission()
                        else onToggleFloating()
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (uiState.isFloatingActive) "اضغط لإيقاف الزر العائم" else "اضغط لتشغيل الزر العائم",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.isFloatingActive) Success400 else TextLight
                )

                Spacer(modifier = Modifier.height(36.dp))

                // ─── Status pill ─────────────────────────────────────
                StatusBanner(isActive = uiState.isFloatingActive)

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Language selection ──────────────────────────────
                SectionCard {
                    Column(modifier = Modifier.padding(18.dp)) {
                        SectionHeader(icon = Icons.Outlined.Translate, title = "إعدادات اللغة")
                        Spacer(modifier = Modifier.height(16.dp))
                        LanguageDropdown(
                            label = "لغة المصدر",
                            selectedCode = uiState.sourceLang,
                            languages = LanguageMapper.supportedLanguages,
                            onSelected = onSourceLangChanged
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LanguageDropdown(
                            label = "لغة الهدف",
                            selectedCode = uiState.targetLang,
                            languages = LanguageMapper.targetLanguages,
                            onSelected = onTargetLangChanged
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Model download (full-width, with real loading state) ─
                ModelDownloadCard(
                    isLoading = uiState.isDownloadingModel,
                    message = uiState.downloadMessage,
                    onClick = onDownloadModels
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Quick actions ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAction(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.History,
                        label = "السجل",
                        onClick = onNavigateToHistory
                    )
                    QuickAction(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Tune,
                        label = "الإعدادات",
                        onClick = onNavigateToSettings
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ─── Privacy badge ───────────────────────────────────
                PrivacyBadge()

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

// ─── Components ──────────────────────────────────────────────────────────────

@Composable
private fun AmbientGlow(isActive: Boolean) {
    val transition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val accent = if (isActive) Success400 else Ember500

    Box(modifier = Modifier.fillMaxSize()) {
        // Top ember bloom behind the hero
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 110.dp)
                .size(360.dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha * 0.5f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
private fun BrandHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulse mark
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(listOf(Ember400, Ember600))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = Ink900,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "نبض",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )
            Text(
                text = "ترجمة الشاشة",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = TextMuted)
        }
    }
}

@Composable
private fun MainActionButton(isActive: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    val accent = if (isActive) Success400 else Ember500
    val pressScale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "press"
    )

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Expanding pulse ring (the "نبض")
        Box(
            modifier = Modifier
                .size(132.dp)
                .scale(ringScale)
                .clip(CircleShape)
                .background(accent.copy(alpha = ringAlpha))
        )
        // Soft static glow
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
        )
        // Core button
        Box(
            modifier = Modifier
                .size(116.dp)
                .scale(pressScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (isActive)
                            listOf(Glass700, Glass800)
                        else
                            listOf(Ember400, Ember600)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.3f))),
                    shape = CircleShape
                )
                .then(Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.Stop else Icons.Filled.CenterFocusStrong,
                contentDescription = if (isActive) "إيقاف" else "تشغيل",
                modifier = Modifier.size(46.dp),
                tint = if (isActive) Success400 else Ink900
            )
        }
    }
}

@Composable
private fun StatusBanner(isActive: Boolean) {
    val bg = if (isActive) Success400.copy(alpha = 0.1f) else Glass800.copy(alpha = 0.7f)
    val border = if (isActive) Success400.copy(alpha = 0.45f) else GlassBorder.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulsingDot(active = isActive)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isActive) "الزر العائم نشط" else "الزر العائم متوقف",
            style = MaterialTheme.typography.titleSmall,
            color = if (isActive) Success400 else TextLight,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (isActive) "يعمل الآن" else "معطّل",
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) Success400.copy(alpha = 0.8f) else TextDim
        )
    }
}

@Composable
private fun PulsingDot(active: Boolean) {
    if (!active) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(TextDim)
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(Success400.copy(alpha = alpha))
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Glass800.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(EmberSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Ember400, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite
        )
    }
}

@Composable
private fun ModelDownloadCard(
    isLoading: Boolean,
    message: String?,
    onClick: () -> Unit
) {
    val success = message?.contains("✓") == true
    SectionCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(EmberSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Ember400
                        )
                    } else {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = Ember400,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نماذج الترجمة",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextLight
                    )
                    Text(
                        text = when {
                            isLoading -> "جارٍ التحميل..."
                            message != null -> message
                            else -> "حمّلها مرة واحدة لتعمل دون إنترنت"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            success -> Success400
                            message != null && !isLoading -> Error400
                            else -> TextDim
                        }
                    )
                }
                Button(
                    onClick = onClick,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ember500,
                        contentColor = Ink900,
                        disabledContainerColor = Glass600
                    )
                ) {
                    Text(
                        text = if (isLoading) "..." else "تحميل",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Glass800.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Ember400,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextLight
            )
        }
    }
}

@Composable
private fun PrivacyBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Success400.copy(alpha = 0.06f))
            .border(1.dp, Success400.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.VerifiedUser,
            contentDescription = "خصوصية",
            tint = Success400,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "لا تُحفظ صور الشاشة ولا تُرسل لأي خادم",
            style = MaterialTheme.typography.bodySmall,
            color = TextLight
        )
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
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ember500,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = Glass700.copy(alpha = 0.4f),
                unfocusedContainerColor = Glass700.copy(alpha = 0.25f),
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextLight,
                focusedLabelColor = Ember400,
                cursorColor = Ember500
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Glass800)
        ) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                lang.nativeName,
                                modifier = Modifier.weight(1f),
                                color = if (lang.code == selectedCode) Ember400 else TextLight,
                                style = MaterialTheme.typography.bodyLarge
                            )
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
