package luzzr.xi.feature.translate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.R

@Composable
internal fun LanguageSelector(language: SupportedLanguage, onClick: () -> Unit) {
    PressScaleBox(
        onClick = onClick,
        onPressScale = 0.97f,
        modifier = Modifier
            .clip(AppShape.small)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = AppSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(language.nativeName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            AbstractIcons.ArrowDropDown(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LanguagePickerSheet(title: String, currentLang: SupportedLanguage, excludeLang: SupportedLanguage, onSelect: (SupportedLanguage) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(AppSpacing.md))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.settings_search_placeholder), color = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                shape = AppShape.small
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            val languages = SupportedLanguage.entries.filter { it != excludeLang && (searchQuery.isBlank() || it.nativeName.contains(searchQuery, ignoreCase = true) || it.displayName.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)) }
            val rows = languages.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()).padding(bottom = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                for (rowItems in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        for (lang in rowItems) {
                            val isSelected = lang == currentLang
                            PressScaleBox(
                                onClick = { onSelect(lang) },
                                onPressScale = 0.97f,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(AppShape.small)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp, vertical = AppSpacing.md)
                                    .semantics { contentDescription = lang.nativeName },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text(lang.nativeName, style = MaterialTheme.typography.titleSmall, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground)
                                    Text(lang.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        val emptySlots = 2 - rowItems.size
                        if (emptySlots > 0) {
                            Spacer(modifier = Modifier.weight(emptySlots.toFloat()))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
