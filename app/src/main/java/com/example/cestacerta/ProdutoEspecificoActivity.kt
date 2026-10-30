package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.util.Locale

class ProdutoEspecificoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PRODUTO = "produto"
    }

    data class PrecoMercado(val mercado: String, val logo: Int, val preco: Double)

    private val formatoReal = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private lateinit var tvProduto: TextView
    private lateinit var listaMercados: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_produto_especifico)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.produto_especifico)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvProduto = findViewById(R.id.tvProduto)
        listaMercados = findViewById(R.id.listaMercados)

        mostrarProduto(intent.getStringExtra(EXTRA_PRODUTO) ?: "Detergente Candura")

        val campoBusca = findViewById<EditText>(R.id.campoBusca)
        campoBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val texto = campoBusca.text.toString().trim()
                if (texto.isNotEmpty()) mostrarProduto(texto)
                true
            } else {
                false
            }
        }

        configurarBarraInferior()
    }

    private fun mostrarProduto(produto: String) {
        tvProduto.text = produto

        val precos = listOf(
            PrecoMercado("Viva Fresco", R.drawable.mercado_viva_fresco, 2.19),
            PrecoMercado("Mercado Central", R.drawable.mercado_central, 2.87),
            PrecoMercado("Hipermais", R.drawable.mercado_hipermais, 3.20)
        ).sortedBy { it.preco }

        listaMercados.removeAllViews()
        for (item in precos) {
            val cartao = layoutInflater.inflate(R.layout.item_mercado_preco, listaMercados, false)
            cartao.findViewById<ImageView>(R.id.ivLogoMercado).setImageResource(item.logo)
            cartao.findViewById<TextView>(R.id.tvNomeMercado).text = item.mercado
            cartao.findViewById<TextView>(R.id.tvPreco).text = formatoReal.format(item.preco)

            cartao.setOnClickListener {
                val abrir = Intent(this, MercadoEspecificoActivity::class.java)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_NOME, item.mercado)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_LOGO, item.logo)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_ENDERECO, item.mercado)
                startActivity(abrir)
            }
            listaMercados.addView(cartao)
        }
    }
}
