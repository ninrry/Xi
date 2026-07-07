package luzzr.xi.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import luzzr.xi.BuildConfig
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AppSpacing

@Composable
fun SettingsAboutSection() {
    SectionCard {
        SectionTitle(stringResource(R.string.settings_section_about))
        Text(
            "${stringResource(R.string.settings_version)} v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.settings_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            stringResource(R.string.settings_default_model_info),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )
    }
}
