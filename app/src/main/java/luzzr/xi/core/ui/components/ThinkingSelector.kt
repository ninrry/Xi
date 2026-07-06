package luzzr.xi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.model.displayNameRes
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.R
import androidx.compose.material3.MaterialTheme

@Composable
fun ThinkingSelector(
    currentLevel: ThinkingLevel,
    onLevelChange: (ThinkingLevel) -> Unit,
    label: String = stringResource(R.string.thinking_label),
    modifier: Modifier = Modifier
) {
    val lowName = stringResource(R.string.thinking_low_name)
    val medName = stringResource(R.string.thinking_medium_name)
    val highName = stringResource(R.string.thinking_high_name)
    val nameMap = mapOf(
        ThinkingLevel.LOW to lowName,
        ThinkingLevel.MEDIUM to medName,
        ThinkingLevel.HIGH to highName
    )

    CollapsibleSelector(
        currentValue = currentLevel,
        values = ThinkingLevel.entries,
        onValueChange = onLevelChange,
        label = label,
        getDisplayName = { nameMap[it] ?: it.displayName },
        icon = {
            AbstractIcons.Sparkle(modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        },
        modifier = modifier
    )
}
