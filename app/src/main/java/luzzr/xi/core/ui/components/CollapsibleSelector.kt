package luzzr.xi.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape

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
    val arrowRotation by animateFloatAsState(
        targetValue = if (showOptions) 180f else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "arrow_rotation"
    )

    Column(
        modifier = modifier
            .clip(AppShape.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { showOptions = !showOptions })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    getDisplayName(currentValue), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                AbstractIcons.ArrowDropDown(
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                        .semantics { contentDescription = if (showOptions) "收起选项" else "展开选项" },
                    tint = MaterialTheme.colorScheme.primary
                )
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                values.forEach { value ->
                    val isSelected = value == currentValue
                    Box(
                        modifier = Modifier
                            .clip(AppShape.small)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                            .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, AppShape.small)
                            .clickable(onClick = { onValueChange(value); showOptions = false })
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .semantics { contentDescription = getDisplayName(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getDisplayName(value),
                            fontSize = 12.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
