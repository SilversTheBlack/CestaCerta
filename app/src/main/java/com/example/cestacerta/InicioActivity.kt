package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class InicioActivity : AppCompatActivity() {
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

        val categorias = mapOf(
            R.id.catHigiene to "Higiene",
            R.id.catLimpeza to "Limpeza",
            R.id.catHortifruti to "Hortifruti",
            R.id.catFrios to "Frios e laticínios",
            R.id.catTemperos to "Temperos"
        )
        for ((id, nome) in categorias) {
            findViewById<View>(id).setOnClickListener {
                val abrir = Intent(this, CategoriaActivity::class.java)
                abrir.putExtra(CategoriaActivity.EXTRA_CATEGORIA, nome)
                startActivity(abrir)
            }
        }

        findViewById<ImageButton>(R.id.btnInicio).setOnClickListener {

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