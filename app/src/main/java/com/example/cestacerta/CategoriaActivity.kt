package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CategoriaActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CATEGORIA = "categoria"
        private const val LIMITE_MERCADOS = 20
    }

    private lateinit var nomeCategoria: String
    private lateinit var listaRanking: LinearLayout
    private lateinit var tvStatusRanking: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categoria)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoria)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nomeCategoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: "Higiene"
        findViewById<TextView>(R.id.txtCategoria).text = nomeCategoria

        val campoBusca = findViewById<EditText>(R.id.campoBusca)
        campoBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val texto = campoBusca.text.toString().trim()
                if (texto.isNotEmpty()) {
                    val abrir = Intent(this, ProdutoEspecificoActivity::class.java)
                    abrir.putExtra(ProdutoEspecificoActivity.EXTRA_PRODUTO, texto)
                    startActivity(abrir)
                }
                true
            } else {
                false
            }
        }

        listaRanking = findViewById(R.id.listaRanking)
        tvStatusRanking = findViewById(R.id.tvStatusRanking)

        lifecycleScope.launch { carregarRanking() }

        configurarBarraInferior()
    }

    private suspend fun carregarRanking() {
        mostrarStatus("Carregando mercados...")
        try {
            // PASSO 1: descobrir o id da categoria a partir do nome que veio da tela anterior.
            // SQL equivalente: select id, nome, icone from categorias where nome ilike 'Higiene' limit 1
            // (ilike = igual, mas sem diferenciar maiúsculas de minúsculas)
            val categoria = supabase.from("categorias")
                .select(Columns.list("id", "nome", "icone")) {
                    filter { ilike("nome", nomeCategoria) }
                    limit(1)
                }
                .decodeSingleOrNull<Categoria>()

            if (categoria == null) {
                mostrarStatus("Categoria \"$nomeCategoria\" não encontrada.")
                return
            }

            // PASSO 2: buscar o ranking dessa categoria, do mais barato para o mais caro.
            // SQL equivalente:
            //   select empresa_id, empresa_nome, endereco, indice_preco, qtd_produtos
            //   from ranking_categoria where categoria_id = ? order by indice_preco limit 20
            val ranking = supabase.from("ranking_categoria")
                .select(Columns.list("empresa_id", "empresa_nome", "endereco", "indice_preco", "qtd_produtos")) {
                    filter { eq("categoria_id", categoria.id) }
                    order("indice_preco", Order.ASCENDING)
                    limit(LIMITE_MERCADOS.toLong())
                }
                .decodeList<RankingCategoria>()

            // PASSO 3: criar um card para cada mercado do ranking
            listaRanking.removeAllViews()
            ranking.forEachIndexed { posicao, item ->
                listaRanking.addView(criarCardMercado(item, posicao))
            }
            if (ranking.isEmpty()) {
                mostrarStatus("Nenhum mercado tem preços nessa categoria ainda.")
            } else {
                tvStatusRanking.visibility = View.GONE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mostrarErro()
        }
    }

    private fun criarCardMercado(item: RankingCategoria, posicao: Int): View {
        // Reaproveita o molde item_mercado_preco.xml (o mesmo usado na tela de Produto Específico)
        val card = layoutInflater.inflate(R.layout.item_mercado_preco, listaRanking, false)

        card.findViewById<ImageView>(R.id.ivLogoMercado).setImageResource(R.drawable.ic_localizacao)
        card.findViewById<TextView>(R.id.tvNomeMercado).text = item.empresaNome
        card.findViewById<TextView>(R.id.tvPreco).text = textoIndice(item.indicePreco)

        // No design original o 1º card fica a 12dp do cabeçalho (o molde usa 22dp entre cards)
        if (posicao == 0) {
            (card.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                (12 * resources.displayMetrics.density).toInt()
        }

        card.setOnClickListener {
            val abrir = Intent(this, MercadoEspecificoActivity::class.java)
            abrir.putExtra(MercadoEspecificoActivity.EXTRA_NOME, item.empresaNome)
            abrir.putExtra(MercadoEspecificoActivity.EXTRA_ENDERECO, item.endereco)
            abrir.putExtra(MercadoEspecificoActivity.EXTRA_LOGO, R.drawable.ic_localizacao)
            startActivity(abrir)
        }
        return card
    }

    // Índice 100 = sempre o menor preço; 112.5 = em média 12,5% acima do menor preço
    private fun textoIndice(indice: Double): String {
        val acima = indice - 100
        return if (acima < 0.05) {
            "Menor preço"
        } else {
            String.format(Locale("pt", "BR"), "+%.1f%%", acima)
        }
    }

    private fun mostrarStatus(texto: String) {
        tvStatusRanking.text = texto
        tvStatusRanking.visibility = View.VISIBLE
        tvStatusRanking.setOnClickListener(null)
    }

    // Mostra a mensagem de erro; ao tocar nela, tenta carregar de novo
    private fun mostrarErro() {
        tvStatusRanking.text = "Não foi possível carregar. Toque para tentar de novo."
        tvStatusRanking.visibility = View.VISIBLE
        tvStatusRanking.setOnClickListener {
            lifecycleScope.launch { carregarRanking() }
        }
    }
}
