package luzzr.xi.core.provider

import luzzr.xi.R

object ProviderRegistry {

    val OPENCODE = ProviderConfig(
        id = "opencode",
        displayNameRes = R.string.provider_opencode,
        descriptionRes = R.string.provider_opencode_desc,
        providerName = "OpenCode Go",
        defaultBaseUrl = "https://opencode.ai/zen/go/v1",
        defaultModel = "mimo-v2.5",
        authType = AuthType.BEARER,
        authHeaderName = "Authorization",
        authHeaderValuePrefix = "Bearer ",
        supportsModelListing = true,
        isBuiltIn = true,
        websiteUrl = "https://opencode.ai",
        helpLinkTextRes = R.string.provider_opencode_get_key
    )

    val XIAOMI_MIMO = ProviderConfig(
        id = "xiaomi_mimo",
        displayNameRes = R.string.provider_xiaomi_mimo,
        descriptionRes = R.string.provider_xiaomi_mimo_desc,
        providerName = "Xiaomi MiMo",
        defaultBaseUrl = "https://api.xiaomimimo.com/v1",
        defaultModel = "mimo-v2.5",
        authType = AuthType.API_KEY_HEADER,
        authHeaderName = "api-key",
        authHeaderValuePrefix = "",
        supportsModelListing = true,
        isBuiltIn = true,
        websiteUrl = "https://mimo.mi.com",
        helpLinkTextRes = R.string.provider_xiaomi_mimo_get_key
    )

    val CUSTOM = ProviderConfig(
        id = "custom",
        displayNameRes = R.string.provider_custom,
        descriptionRes = R.string.provider_custom_desc,
        providerName = "Custom",
        defaultBaseUrl = "",
        defaultModel = "",
        authType = AuthType.BEARER,
        authHeaderName = "Authorization",
        authHeaderValuePrefix = "Bearer ",
        supportsModelListing = true,
        isBuiltIn = true,
        websiteUrl = ""
    )

    val builtInProviders = listOf(OPENCODE, XIAOMI_MIMO, CUSTOM)

    private val providerMap = builtInProviders.associateBy { it.id }

    fun getProvider(id: String): ProviderConfig = providerMap[id] ?: CUSTOM

    fun getAllProviders(): List<ProviderConfig> = builtInProviders
}
