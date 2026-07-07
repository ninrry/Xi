package luzzr.xi.domain.repository

import luzzr.xi.domain.model.ModelListResponse

interface SettingsGateway {
    suspend fun testConnection(): ModelListResponse
}
