package luzzr.xi.feature.essay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import androidx.compose.material3.MaterialTheme
import luzzr.xi.core.ui.theme.AppShape

@Composable
fun CorrectButton(isLoading: Boolean, onClick: () -> Unit, onCancel: () -> Unit = {}, modifier: Modifier = Modifier) {
    if (isLoading) {
        PressScaleBox(
            onClick = onCancel,
            onPressScale = 0.97f,
            modifier = modifier
                .height(44.dp)
                .clip(AppShape.button)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AbstractIcons.Stop(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.background)
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(stringResource(R.string.permission_cancel), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.background)
            }
        }
    } else {
        PressScaleBox(
            onClick = onClick,
            onPressScale = 0.97f,
            modifier = modifier
                .height(44.dp)
                .clip(AppShape.button)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.essay_correct_btn), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
fun EssayEmptyState(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        AbstractIcons.Edit(Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
    }
}
