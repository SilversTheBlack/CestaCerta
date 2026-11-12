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

// Classes de leitura usadas pela tela de Início.
// Cada uma representa uma linha de uma tabela do Supabase: os nomes das
// propriedades são iguais aos das colunas, porque o Supabase devolve JSON
// e o @Serializable converte JSON -> objeto Kotlin.

@Serializable
data class Categoria(
    val id: Long,
    val nome: String,
    val icone: String? = null
)

@Serializable
data class Mercado(
    val id: Long,
    val nome: String,
    val endereco: String
)

// Uma linha da view "ranking_categoria" (tela de Categoria).
// @SerialName liga o nome da coluna no banco (com _) ao nome da propriedade em Kotlin.
@Serializable
data class RankingCategoria(
    @SerialName("empresa_id") val empresaId: Long,
    @SerialName("empresa_nome") val empresaNome: String,
    val endereco: String,
    @SerialName("indice_preco") val indicePreco: Double,
    @SerialName("qtd_produtos") val qtdProdutos: Long
)