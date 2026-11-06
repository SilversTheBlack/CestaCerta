package com.example.cestacerta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class ProdutosMercadoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_NOME = "nome_mercado"
        const val EXTRA_LOGO = "logo_mercado"
        const val EXTRA_CATEGORIA = "categoria"
    }

    data class Produto(
        val nome: String,
        val preco: Double,
        val imagem: Int = R.drawable.ic_produto,
        val foto: Uri? = null
    )

    private val formatoReal = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private val produtos = mutableListOf(
        Produto("Detergente Silva", 2.19),
        Produto("Detergente Caetano", 3.09),
        Produto("Detergente Peixoto", 3.15),
        Produto("Detergente Polvovisky", 1.99),
        Produto("Detergente TDS", 0.99)
    )

    private lateinit var listaProdutos: LinearLayout
    private lateinit var tvListaVazia: TextView
    private lateinit var campoBusca: EditText

    private lateinit var nomeMercado: String
    private var logoMercado = 0

    private var fotoEscolhida: Uri? = null
    private var textoInserirImagem: TextView? = null

    private val escolherFoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                fotoEscolhida = uri
                textoInserirImagem?.text = "Imagem selecionada"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_produtos_mercado)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.produtos_mercado)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nomeMercado = intent.getStringExtra(EXTRA_NOME) ?: "Hipermais"
        logoMercado = intent.getIntExtra(EXTRA_LOGO, R.drawable.mercado_hipermais)
        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: ""

        listaProdutos = findViewById(R.id.listaProdutos)
        tvListaVazia = findViewById(R.id.tvListaVazia)
        campoBusca = findViewById(R.id.campoBusca)

        findViewById<ImageView>(R.id.ivLogoMercado).setImageResource(logoMercado)
        if (categoria.isNotEmpty()) campoBusca.hint = "Pesquise em $categoria"

        campoBusca.doAfterTextChanged { mostrarProdutos() }

        findViewById<MaterialButton>(R.id.btnCadastrarProduto).setOnClickListener {
            exigirLogin { abrirPopupCadastro() }
        }

        mostrarProdutos()
        configurarBarraInferior()
    }

    private fun mostrarProdutos() {
        val busca = campoBusca.text.toString().trim()
        val filtrados = produtos.filter { it.nome.contains(busca, ignoreCase = true) }

        listaProdutos.removeAllViews()
        for (produto in filtrados) {
            val cartao = layoutInflater.inflate(R.layout.item_produto, listaProdutos, false)
            val ivProduto = cartao.findViewById<ImageView>(R.id.ivProduto)
            if (produto.foto != null) ivProduto.setImageURI(produto.foto) else ivProduto.setImageResource(produto.imagem)
            cartao.findViewById<TextView>(R.id.tvNomeProduto).text = produto.nome
            cartao.findViewById<TextView>(R.id.tvPreco).text = formatoReal.format(produto.preco)

            cartao.setOnClickListener {
                val abrir = Intent(this, ProdutoEspecificoActivity::class.java)
                abrir.putExtra(ProdutoEspecificoActivity.EXTRA_PRODUTO, produto.nome)
                startActivity(abrir)
            }
            listaProdutos.addView(cartao)
        }
        tvListaVazia.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun abrirPopupCadastro() {
        val view = layoutInflater.inflate(R.layout.dialog_cadastro_produto, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()

        fotoEscolhida = null
        view.findViewById<ImageView>(R.id.ivLogoMercado).setImageResource(logoMercado)
        view.findViewById<TextView>(R.id.tvNomeMercado).text = nomeMercado

        textoInserirImagem = view.findViewById(R.id.btnInserirImagem)
        textoInserirImagem?.setOnClickListener {
            escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val etNome = view.findViewById<EditText>(R.id.etNomeProduto)
        val etPreco = view.findViewById<EditText>(R.id.etPreco)

        view.findViewById<MaterialButton>(R.id.btnSalvar).setOnClickListener {
            val nome = etNome.text.toString().trim()
            val preco = converterPreco(etPreco.text.toString())

            if (nome.isEmpty()) {
                etNome.error = "Informe o nome do produto"
                etNome.requestFocus()
                return@setOnClickListener
            }
            if (preco == null || preco <= 0) {
                etPreco.error = "Preço inválido"
                etPreco.requestFocus()
                return@setOnClickListener
            }

            produtos.add(Produto(nome, preco, foto = fotoEscolhida))
            mostrarProdutos()
            Toast.makeText(this, "Produto cadastrado!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setOnDismissListener { textoInserirImagem = null }
        dialog.show()
    }

    private fun converterPreco(texto: String): Double? {
        return texto.replace("R$", "")
            .replace(" ", "")
            .replace(",", ".")
            .toDoubleOrNull()
    }
}
