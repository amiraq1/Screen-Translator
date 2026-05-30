package com.ammar.nabdscreentranslate.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ammar.nabdscreentranslate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOverlayOpacityChanged: (Float) -> Unit,
    onSaveHistoryChanged: (Boolean) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onVibrateChanged: (Boolean) -> Unit,
    onLightBgChanged: (Boolean) -> Unit,
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
                        icon = Icons.Outlined.FormatColorFill,
                        title = "خلفية خفيفة خلف الترجمة",
                        subtitle = "تظهر الترجمة كنص فقط فوق الشاشة افتراضيًا",
                        checked = uiState.lightBgBehindTranslation,
                        onCheckedChange = onLightBgChanged
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
