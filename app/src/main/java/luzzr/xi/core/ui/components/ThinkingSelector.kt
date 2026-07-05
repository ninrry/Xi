package luzzr.xi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.core.ui.theme.AbstractIcons
import androidx.compose.material3.MaterialTheme

@Composable
fun ThinkingSelector(
    currentLevel: ThinkingLevel,
    onLevelChange: (ThinkingLevel) -> Unit,
    label: String = "思考程度",
    modifier: Modifier = Modifier
) {
    CollapsibleSelector(
        currentValue = currentLevel,
        values = ThinkingLevel.entries,
        onValueChange = onLevelChange,
        label = label,
        getDisplayName = { it.displayName },
        icon = {
            AbstractIcons.Sparkle(modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        },
        modifier = modifier
    )
}
