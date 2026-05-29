package com.ammar.nabdscreentranslate.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammar.nabdscreentranslate.data.TranslationHistoryEntity
import com.ammar.nabdscreentranslate.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyItems: List<TranslationHistoryEntity>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onDeleteItem: (TranslationHistoryEntity) -> Unit,
    onDeleteAll: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("سجل الترجمات", fontWeight = FontWeight.Bold, color = TextWhite)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", tint = TextLight)
                        }
                    },
                    actions = {
                        if (historyItems.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "حذف الكل", tint = Error400)
                            }
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
            ) {
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("بحث في الترجمات...", color = TextDim) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Filled.Clear, "مسح", tint = TextMuted)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Glass800.copy(alpha = 0.5f),
                        unfocusedContainerColor = Glass800.copy(alpha = 0.3f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextLight
                    )
                )

                if (historyItems.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextDim.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لا توجد ترجمات بعد",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط الزر العائم لترجمة أي نص يظهر على الشاشة",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDim,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyItems, key = { it.id }) { item ->
                            HistoryItemCard(item = item, onDelete = { onDeleteItem(item) })
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = Glass800,
            title = { Text("حذف الكل", color = TextWhite) },
            text = { Text("هل تريد حذف جميع الترجمات المحفوظة؟", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { onDeleteAll(); showDeleteAllDialog = false }) {
                    Text("حذف", color = Error400)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(item: TranslationHistoryEntity, onDelete: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Glass800.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Source text
            Text(
                text = item.sourceText,
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Translated text
            Text(
                text = item.translatedText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(item.translatedText)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, "نسخ", Modifier.size(16.dp), tint = Cyan400)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, "حذف", Modifier.size(16.dp), tint = Error400.copy(alpha = 0.7f))
                }
            }
        }
    }
}
