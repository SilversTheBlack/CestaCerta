package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CategoriaActivity : AppCompatActivity() {

    companion object {
        // Nome da "etiqueta" usada para mandar a categoria de uma tela para outra
        const val EXTRA_CATEGORIA = "categoria"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categoria)

        // Evita que o conteúdo fique embaixo da barra de status / navegação do celular
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoria)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mostra a categoria que veio da tela Início (se não vier nada, fica "Higiene")
        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: "Higiene"
        findViewById<TextView>(R.id.txtCategoria).text = categoria

        // Busca por produto
        val campoBusca = findViewById<EditText>(R.id.campoBusca)
        campoBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val texto = campoBusca.text.toString().trim()
                if (texto.isNotEmpty()) {
                    Toast.makeText(this, "Buscando: $texto (em breve)", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        // Mercados. Por enquanto os preços estão fixos no layout;
        // quando a lista de produtos existir, troque o Toast por startActivity(...)
        val mercados = mapOf(
            R.id.mercadoVivaFresco to "Viva Fresco",
            R.id.mercadoCentral to "Mercado Central",
            R.id.mercadoHipermais to "Hipermais"
        )
        for ((id, nome) in mercados) {
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "$nome - $categoria (em breve)", Toast.LENGTH_SHORT).show()
            }
        }

        // Barra de baixo
        findViewById<ImageButton>(R.id.btnInicio).setOnClickListener {
            finish() // volta para a tela Início
        }
        findViewById<ImageButton>(R.id.btnNotificacoes).setOnClickListener {
            Toast.makeText(this, "Notificações (em breve)", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnSobre).setOnClickListener {
            startActivity(Intent(this, SobreNosActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.btnPerfil).setOnClickListener {
            Toast.makeText(this, "Perfil (em breve)", Toast.LENGTH_SHORT).show()
        }
    }
}