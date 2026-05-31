package com.ammar.nabdscreentranslate.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ammar.nabdscreentranslate.data.SettingsDataStore
import com.ammar.nabdscreentranslate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOverlayOpacityChanged: (Float) -> Unit,
    onSaveHistoryChanged: (Boolean) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onVibrateChanged: (Boolean) -> Unit,
    onDisplayModeChanged: (String) -> Unit,
    onDeclutterChanged: (Boolean) -> Unit,
    onPolishArabicChanged: (Boolean) -> Unit,
    onLiveTranslationChanged: (Boolean) -> Unit,
    onLiveIntervalChanged: (String) -> Unit,
    onLiveOnlyOnChangeChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("الإعدادات", fontWeight = FontWeight.Bold, color = TextWhite) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "رجوع", tint = TextLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink900)
                )
            },
            containerColor = Ink900
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ─── Appearance ───────────────────────────────────────
                SettingsSection(title = "المظهر", icon = Icons.Outlined.Palette) {
                    SettingsToggle(
                        icon = Icons.Outlined.DarkMode,
                        title = "الوضع الداكن",
                        subtitle = "تفعيل المظهر الداكن",
                        checked = uiState.darkMode,
                        onCheckedChange = onDarkModeChanged
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Translation Overlay ─────────────────────────────
                SettingsSection(title = "نافذة الترجمة", icon = Icons.Outlined.Layers) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "طريقة عرض الترجمة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DisplayModeSelector(
                            selected = uiState.displayMode,
                            onSelected = onDisplayModeChanged
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Opacity, null, tint = Ember400, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("شفافية النافذة", style = MaterialTheme.typography.bodyMedium, color = TextLight, modifier = Modifier.weight(1f))
                            Text("${(uiState.overlayOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextDim)
                        }
                        Slider(
                            value = uiState.overlayOpacity,
                            onValueChange = onOverlayOpacityChanged,
                            valueRange = 0.5f..1f,
                            modifier = Modifier.padding(top = 4.dp),
                            colors = SliderDefaults.colors(thumbColor = Ember500, activeTrackColor = Ember500, inactiveTrackColor = Glass600)
                        )
                    }
                    Divider(color = GlassBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggle(
                        icon = Icons.Outlined.CleaningServices,
                        title = "تقليل ازدحام الترجمة",
                        subtitle = "تجميع النصوص القريبة وتصفية العناصر غير المهمة",
                        checked = uiState.declutterOverlay,
                        onCheckedChange = onDeclutterChanged
                    )
                    Divider(color = GlassBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggle(
                        icon = Icons.Outlined.Translate,
                        title = "تحسين العربية",
                        subtitle = "تصحيح الترجمة وتحسين الأسلوب العربي",
                        checked = uiState.polishArabic,
                        onCheckedChange = onPolishArabicChanged
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Live Translation ────────────────────────────────
                SettingsSection(title = "الترجمة الفورية", icon = Icons.Outlined.PlayCircle) {
                    SettingsToggle(
                        icon = Icons.Outlined.PlayCircle,
                        title = "تشغيل الترجمة الفورية",
                        subtitle = "ترجمة مستمرة عند تغيّر النص",
                        checked = uiState.liveTranslation,
                        onCheckedChange = onLiveTranslationChanged
                    )
                    Divider(color = GlassBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "سرعة التحديث",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LiveIntervalSelector(
                            selected = uiState.liveInterval,
                            onSelected = onLiveIntervalChanged
                        )
                    }
                    Divider(color = GlassBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggle(
                        icon = Icons.Outlined.ChangeCircle,
                        title = "ترجمة عند تغيّر النص فقط",
                        subtitle = "تخطي الترجمة إذا النص لم يتغير",
                        checked = uiState.liveOnlyOnChange,
                        onCheckedChange = onLiveOnlyOnChangeChanged
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Behavior ────────────────────────────────────────
                SettingsSection(title = "السلوك", icon = Icons.Outlined.Tune) {
                    SettingsToggle(
                        icon = Icons.Outlined.Save,
                        title = "حفظ السجل",
                        subtitle = "حفظ الترجمات تلقائيًا",
                        checked = uiState.saveHistory,
                        onCheckedChange = onSaveHistoryChanged
                    )
                    Divider(color = GlassBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggle(
                        icon = Icons.Outlined.Vibration,
                        title = "اهتزاز عند الترجمة",
                        subtitle = "اهتزاز خفيف عند إتمام الترجمة",
                        checked = uiState.vibrateOnTranslate,
                        onCheckedChange = onVibrateChanged
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Privacy ─────────────────────────────────────────
                SettingsSection(title = "الخصوصية", icon = Icons.Outlined.Shield) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val items = listOf(
                            "لا يتم حفظ صور الشاشة",
                            "لا يتم إرسال البيانات لأي خادم",
                            "تتم المعالجة على جهازك فقط",
                            "لا يتم استخدام خدمات إمكانية الوصول"
                        )
                        items.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Success400)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Nabd Screen Translate v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DisplayModeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf(
        SettingsDataStore.DISPLAY_MODE_OVERLAY to "فوق النص",
        SettingsDataStore.DISPLAY_MODE_SHEET to "لوحة سفلية",
        SettingsDataStore.DISPLAY_MODE_BOTH to "كلاهما",
        SettingsDataStore.DISPLAY_MODE_VISUAL_REPLACE to "استبدال بصري"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Glass700.copy(alpha = 0.4f))
            .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // First row: 3 options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.take(3).forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) Ember500 else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelected(value) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Ink900 else TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // Second row: visual replace option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.drop(3).forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) Ember500 else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelected(value) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Ink900 else TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // Fill remaining space
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun LiveIntervalSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf(
        SettingsDataStore.LIVE_INTERVAL_FAST to "سريع (1ث)",
        SettingsDataStore.LIVE_INTERVAL_BALANCED to "متوازن (2ث)",
        SettingsDataStore.LIVE_INTERVAL_BATTERY to "توفير (3ث)"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Glass700.copy(alpha = 0.4f))
            .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) Ember500 else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Ink900 else TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Ember400, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Ember400
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Glass800.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Ember400, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextLight)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink900,
                checkedTrackColor = Ember500,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = Glass600
            )
        )
    }
}
