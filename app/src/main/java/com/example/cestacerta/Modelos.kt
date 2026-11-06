package com.example.cestacerta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovaEmpresa(
    @SerialName("vendedor_id") val vendedorId: String,
    val nome: String,
    val cnpj: String?,
    val endereco: String,
    val latitude: Double,
    val longitude: Double
)
