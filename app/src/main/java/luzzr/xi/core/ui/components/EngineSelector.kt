package luzzr.xi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.displayNameRes
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.R
import androidx.compose.material3.MaterialTheme

@Composable
fun EngineSelector(
    currentEngine: TranslationEngine,
    onEngineChange: (TranslationEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    val aiName = stringResource(R.string.engine_ai_name)
    val mlkitName = stringResource(R.string.engine_mlkit_name)
    val nameMap = mapOf(
        TranslationEngine.AI to aiName,
        TranslationEngine.MLKIT to mlkitName
    )

    CollapsibleSelector(
        currentValue = currentEngine,
        values = TranslationEngine.entries,
        onValueChange = onEngineChange,
        label = stringResource(R.string.engine_label),
        getDisplayName = { nameMap[it] ?: it.displayName },
        icon = {
            AbstractIcons.Translate(modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        },
        modifier = modifier
    )
}
