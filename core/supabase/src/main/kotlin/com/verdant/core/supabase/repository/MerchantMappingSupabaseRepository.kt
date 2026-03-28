package com.verdant.core.supabase.repository

import com.verdant.core.model.MerchantMapping
import com.verdant.core.model.repository.MerchantMappingRepository
import com.verdant.core.supabase.dto.MerchantMappingDto
import com.verdant.core.supabase.dto.toDomain
import com.verdant.core.supabase.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class MerchantMappingSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : MerchantMappingRepository {

    private val table = "merchant_mappings"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun findByMerchant(merchant: String): MerchantMapping? =
        supabase.postgrest[table]
            .select { filter { ilike("merchant_pattern", "%$merchant%") } }
            .decodeList<MerchantMappingDto>()
            .maxByOrNull { it.useCount }
            ?.toDomain()

    override suspend fun upsert(mapping: MerchantMapping) {
        supabase.postgrest[table].upsert(mapping.toDto(userId()))
    }

    override suspend fun incrementUseCount(id: String) {
        val current = supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<MerchantMappingDto>()
        if (current != null) {
            supabase.postgrest[table].update(mapOf("use_count" to current.useCount + 1)) {
                filter { eq("id", id) }
            }
        }
    }

    override suspend fun getAll(): List<MerchantMapping> =
        supabase.postgrest[table]
            .select { order("use_count", Order.DESCENDING) }
            .decodeList<MerchantMappingDto>()
            .map { it.toDomain() }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
