package luzzr.xi.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R

@Composable
fun SettingsAboutSection() {
    SectionCard {
        SectionTitle(stringResource(R.string.settings_section_about))
        Text(
            stringResource(R.string.settings_version),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.settings_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_default_model_info),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )
    }
}
