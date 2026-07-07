package luzzr.xi.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.theme.MotionTokens
import luzzr.xi.core.ui.components.PressScaleBox

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> CollapsibleSelector(
    currentValue: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    label: String,
    getDisplayName: (T) -> String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOptions by remember { mutableStateOf(false) }
    val collapseDescription = stringResource(R.string.semantics_collapse_options)
    val expandDescription = stringResource(R.string.semantics_expand_options)
    val arrowRotation by animateFloatAsState(
        targetValue = if (showOptions) 180f else 0f,
        animationSpec = MotionTokens.springDefault(),
        label = "arrow_rotation"
    )

    Column(
        modifier = modifier
            .clip(AppShape.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card)
    ) {
        PressScaleBox(
            onClick = { showOptions = !showOptions },
            onPressScale = 0.98f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        getDisplayName(currentValue), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    AbstractIcons.ArrowDropDown(
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = arrowRotation }
                            .semantics { contentDescription = if (showOptions) collapseDescription else expandDescription },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showOptions,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = AppSpacing.lg, end = AppSpacing.lg, bottom = AppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                values.forEach { value ->
                    val isSelected = value == currentValue
                    PressScaleBox(
                        onClick = { onValueChange(value); showOptions = false },
                        modifier = Modifier
                            .clip(AppShape.small)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                            .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, AppShape.small)
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                            .semantics { contentDescription = getDisplayName(value) }
                    ) {
                        Text(
                            text = getDisplayName(value),
                            style = if (isSelected) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
