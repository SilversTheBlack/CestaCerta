package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CategoriaActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CATEGORIA = "categoria"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categoria)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoria)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: "Higiene"
        findViewById<TextView>(R.id.txtCategoria).text = categoria

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

        val mercados = listOf(
            Triple(R.id.mercadoVivaFresco, "Viva Fresco", R.drawable.mercado_viva_fresco),
            Triple(R.id.mercadoCentral, "Mercado Central", R.drawable.mercado_central),
            Triple(R.id.mercadoHipermais, "Hipermais", R.drawable.mercado_hipermais)
        )
        for ((id, nome, logo) in mercados) {
            findViewById<View>(id).setOnClickListener {
                val abrir = Intent(this, MercadoEspecificoActivity::class.java)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_NOME, nome)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_LOGO, logo)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_ENDERECO, nome)
                startActivity(abrir)
            }
        }

        configurarBarraInferior()
    }
}