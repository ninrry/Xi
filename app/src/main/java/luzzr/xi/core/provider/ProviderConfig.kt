package luzzr.xi.core.provider

import androidx.annotation.StringRes

enum class AuthType(val id: String) {
    BEARER("bearer"),
    API_KEY_HEADER("api_key_header");

    companion object {
        fun fromId(id: String): AuthType = entries.find { it.id == id } ?: BEARER
    }
}

data class ProviderConfig(
    val id: String,
    @StringRes val displayNameRes: Int,
    @StringRes val descriptionRes: Int,
    val providerName: String = "",
    val defaultBaseUrl: String,
    val defaultModel: String,
    val authType: AuthType,
    val authHeaderName: String = "Authorization",
    val authHeaderValuePrefix: String = "Bearer ",
    val supportsModelListing: Boolean = true,
    val isBuiltIn: Boolean = true,
    val websiteUrl: String = "",
    @StringRes val helpLinkTextRes: Int = 0
)
