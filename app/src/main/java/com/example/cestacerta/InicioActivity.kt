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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class InicioActivity : AppCompatActivity() {
    companion object {
        private const val LIMITE_MERCADOS = 10

        // Ícone de cada categoria (pelo nome que está no banco).
        // Categoria sem ícone próprio usa o ícone genérico de produto.
        private val ICONES_CATEGORIA = mapOf(
            "Higiene" to R.drawable.cat_higiene,
            "Limpeza" to R.drawable.cat_limpeza,
            "Hortifruti" to R.drawable.cat_hortifruti,
            "Laticínios" to R.drawable.cat_frios,
            "Mercearia" to R.drawable.cat_temperos
        )
    }

    private lateinit var listaCategorias: LinearLayout
    private lateinit var listaMercados: LinearLayout
    private lateinit var tvStatusCategorias: TextView
    private lateinit var tvStatusMercados: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_busca_inicio)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.inicio)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val campoBusca = findViewById<EditText>(R.id.campoBusca)
        campoBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val texto = campoBusca.text.toString().trim()
                if (texto.isNotEmpty()) {
                    val abrir = Intent(this, CategoriaActivity::class.java)
                    abrir.putExtra(CategoriaActivity.EXTRA_CATEGORIA, texto)
                    startActivity(abrir)
                }
                true
            } else {
                false
            }
        }

        listaCategorias = findViewById(R.id.listaCategorias)
        listaMercados = findViewById(R.id.listaMercados)
        tvStatusCategorias = findViewById(R.id.tvStatusCategorias)
        tvStatusMercados = findViewById(R.id.tvStatusMercados)

        // As duas buscas rodam ao mesmo tempo (cada launch é uma tarefa separada)
        lifecycleScope.launch { carregarCategorias() }
        lifecycleScope.launch { carregarMercados() }

        configurarBarraInferior()
    }

    // ---------------------------------------------------------------
    // CATEGORIAS
    // ---------------------------------------------------------------
    private suspend fun carregarCategorias() {
        mostrarStatus(tvStatusCategorias, "Carregando categorias...")
        try {
            // SQL equivalente: select id, nome, icone from categorias order by nome
            val categorias = supabase.from("categorias")
                .select(Columns.list("id", "nome", "icone")) {
                    order("nome", Order.ASCENDING)
                }
                .decodeList<Categoria>()

            listaCategorias.removeAllViews()
            categorias.forEachIndexed { posicao, categoria ->
                listaCategorias.addView(criarCardCategoria(categoria, posicao))
            }
            if (categorias.isEmpty()) {
                mostrarStatus(tvStatusCategorias, "Nenhuma categoria cadastrada.")
            } else {
                tvStatusCategorias.visibility = View.GONE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mostrarErro(tvStatusCategorias) { carregarCategorias() }
        }
    }

    private fun criarCardCategoria(categoria: Categoria, posicao: Int): View {
        // "Infla" o molde: cria uma cópia nova do item_categoria_inicio.xml
        val card = layoutInflater.inflate(R.layout.item_categoria_inicio, listaCategorias, false)

        card.findViewById<TextView>(R.id.tvNomeCategoria).text = categoria.nome
        card.findViewById<ImageView>(R.id.ivIconeCategoria)
            .setImageResource(ICONES_CATEGORIA[categoria.nome] ?: R.drawable.ic_produto)

        if (posicao == 0) tirarMargemDeCima(card)

        card.setOnClickListener {
            val abrir = Intent(this, CategoriaActivity::class.java)
            abrir.putExtra(CategoriaActivity.EXTRA_CATEGORIA, categoria.nome)
            startActivity(abrir)
        }
        return card
    }

    // ---------------------------------------------------------------
    // MERCADOS
    // ---------------------------------------------------------------
    private suspend fun carregarMercados() {
        mostrarStatus(tvStatusMercados, "Carregando mercados...")
        try {
            // SQL equivalente: select id, nome, endereco from empresas order by nome limit 10
            val mercados = supabase.from("empresas")
                .select(Columns.list("id", "nome", "endereco")) {
                    order("nome", Order.ASCENDING)
                    limit(LIMITE_MERCADOS.toLong())
                }
                .decodeList<Mercado>()

            listaMercados.removeAllViews()
            mercados.forEachIndexed { posicao, mercado ->
                listaMercados.addView(criarCardMercado(mercado, posicao))
            }
            if (mercados.isEmpty()) {
                mostrarStatus(tvStatusMercados, "Nenhum mercado cadastrado.")
            } else {
                tvStatusMercados.visibility = View.GONE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mostrarErro(tvStatusMercados) { carregarMercados() }
        }
    }

    private fun criarCardMercado(mercado: Mercado, posicao: Int): View {
        val card = layoutInflater.inflate(R.layout.item_mercado_inicio, listaMercados, false)

        card.findViewById<TextView>(R.id.tvNomeMercado).text = mercado.nome
        card.findViewById<TextView>(R.id.tvEnderecoMercado).text = mercado.endereco

        if (posicao == 0) tirarMargemDeCima(card)

        card.setOnClickListener {
            val abrir = Intent(this, MercadoEspecificoActivity::class.java)
            abrir.putExtra(MercadoEspecificoActivity.EXTRA_NOME, mercado.nome)
            abrir.putExtra(MercadoEspecificoActivity.EXTRA_ENDERECO, mercado.endereco)
            abrir.putExtra(MercadoEspecificoActivity.EXTRA_LOGO, R.drawable.ic_localizacao)
            startActivity(abrir)
        }
        return card
    }

    // ---------------------------------------------------------------
    // Ajudantes
    // ---------------------------------------------------------------

    // O molde tem margem em cima (para separar um card do outro); o primeiro não precisa
    private fun tirarMargemDeCima(card: View) {
        (card.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
    }

    private fun mostrarStatus(status: TextView, texto: String) {
        status.text = texto
        status.visibility = View.VISIBLE
        status.setOnClickListener(null)
    }

    // Mostra a mensagem de erro; ao tocar nela, tenta carregar de novo
    private fun mostrarErro(status: TextView, tentarDeNovo: suspend () -> Unit) {
        status.text = "Não foi possível carregar. Toque para tentar de novo."
        status.visibility = View.VISIBLE
        status.setOnClickListener {
            lifecycleScope.launch { tentarDeNovo() }
        }
    }
}