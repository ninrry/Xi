package luzzr.xi.feature.essay.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionAddBg
import luzzr.xi.core.ui.theme.CorrectionNoteBg
import luzzr.xi.core.ui.theme.MotionTokens
import androidx.compose.material3.MaterialTheme

@Composable
fun CopyableHeader(title: String, content: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val copyInteractionSource = remember { MutableInteractionSource() }
    val isCopyPressed by copyInteractionSource.collectIsPressedAsState()
    val copyScale by animateFloatAsState(
        targetValue = if (isCopyPressed) 0.90f else 1f,
        animationSpec = MotionTokens.springDefault(),
        label = "copy_scale"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
        Box(
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = copyScale
                    scaleY = copyScale
                }
                .clip(AppShape.mini)
                .clickable(
                    interactionSource = copyInteractionSource,
                    indication = null,
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("essay", content))
                        copied = true
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (copied) AbstractIcons.CheckCircle(Modifier.size(16.dp), tint = CorrectionAdd)
            else AbstractIcons.Copy(Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
    androidx.compose.runtime.LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
}

@Composable
fun CorrectionTab(content: String) {
    Column(modifier = Modifier.padding(14.dp)) {
        CopyableHeader(stringResource(R.string.essay_tab_corrections), content)
        Spacer(modifier = Modifier.height(6.dp))
        val sections = content.split("--- ").filter { it.isNotBlank() }
        if (sections.isEmpty()) {
            Text(content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 20.sp)
        } else {
            sections.forEach { section ->
                val lines = section.lines()
                val title = lines.firstOrNull()?.trimEnd('-', ' ') ?: ""
                val body = lines.drop(1).joinToString("\n").trim()
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun CorrectedEssayTab(essay: String) {
    Column(modifier = Modifier.padding(14.dp)) {
        CopyableHeader(stringResource(R.string.essay_tab_corrected), essay)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(AppShape.mini).background(CorrectionAddBg).padding(10.dp)) {
            Text(essay, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 20.sp)
        }
    }
}

@Composable
fun WritingTipsTab(tips: String) {
    Column(modifier = Modifier.padding(14.dp)) {
        CopyableHeader(stringResource(R.string.essay_tab_tips), tips)
        Spacer(modifier = Modifier.height(6.dp))
        val tipLines = tips.lines().filter { it.isNotBlank() }
        tipLines.forEach { line ->
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(AppShape.mini).background(CorrectionNoteBg).padding(10.dp)) {
                Text(line.trimStart('-', ' '), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
            }
        }
    }
}


