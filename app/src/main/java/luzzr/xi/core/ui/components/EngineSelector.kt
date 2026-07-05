package luzzr.xi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.core.ui.theme.AbstractIcons
import androidx.compose.material3.MaterialTheme

@Composable
fun EngineSelector(
    currentEngine: TranslationEngine,
    onEngineChange: (TranslationEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSelector(
        currentValue = currentEngine,
        values = TranslationEngine.entries,
        onValueChange = onEngineChange,
        label = "翻译引擎",
        getDisplayName = { it.displayName },
        icon = {
            AbstractIcons.Translate(modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        },
        modifier = modifier
    )
}
