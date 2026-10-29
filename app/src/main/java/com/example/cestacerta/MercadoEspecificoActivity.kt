package com.example.cestacerta

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.util.Locale

class MercadoEspecificoActivity : AppCompatActivity() {

    companion object {
        // Dados que a tela anterior deve mandar no Intent
        const val EXTRA_NOME = "nome_mercado"
        const val EXTRA_LOGO = "logo_mercado"
        const val EXTRA_ENDERECO = "endereco_mercado"
        const val EXTRA_PRECO_HIGIENE = "preco_higiene"
        const val EXTRA_PRECO_LIMPEZA = "preco_limpeza"
        const val EXTRA_PRECO_HORTIFRUTI = "preco_hortifruti"
    }

    private val formatoReal = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mercado_especifico)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mercado_especifico)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // TODO: os valores padrão são só de exemplo, até os dados virem do banco / API
        val nome = intent.getStringExtra(EXTRA_NOME) ?: "Hipermais"
        val logo = intent.getIntExtra(EXTRA_LOGO, R.drawable.mercado_hipermais)
        val endereco = intent.getStringExtra(EXTRA_ENDERECO) ?: "Hipermais Supermercados"

        findViewById<TextView>(R.id.tvNomeMercado).text = nome
        findViewById<ImageView>(R.id.ivLogoMercado).setImageResource(logo)

        mostrarPreco(R.id.tvPrecoHigiene, intent.getDoubleExtra(EXTRA_PRECO_HIGIENE, 60.57))
        mostrarPreco(R.id.tvPrecoLimpeza, intent.getDoubleExtra(EXTRA_PRECO_LIMPEZA, 70.18))
        mostrarPreco(R.id.tvPrecoHortifruti, intent.getDoubleExtra(EXTRA_PRECO_HORTIFRUTI, 50.37))

        // Categorias
        findViewById<LinearLayout>(R.id.itemHigiene).setOnClickListener {
            Toast.makeText(this, "Produtos de Higiene (em breve)", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.itemLimpeza).setOnClickListener {
            Toast.makeText(this, "Produtos de Limpeza (em breve)", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.itemHortifruti).setOnClickListener {
            Toast.makeText(this, "Produtos de Hortifruti (em breve)", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.cardComoChegar).setOnClickListener {
            abrirNoMaps(endereco)
        }

        // Barra de navegação
        findViewById<ImageButton>(R.id.btnInicio).setOnClickListener {
            startActivity(Intent(this, InicioActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.btnNotificacoes).setOnClickListener {
            Toast.makeText(this, "Notificações (em breve)", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnSobre).setOnClickListener {
            startActivity(Intent(this, SobreNosActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnPerfil).setOnClickListener {
            Toast.makeText(this, "Perfil (em breve)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarPreco(idTexto: Int, preco: Double) {
        findViewById<TextView>(idTexto).text = formatoReal.format(preco)
    }

    // Abre o app do Google Maps (ou o navegador, se o app não estiver instalado)
    private fun abrirNoMaps(endereco: String) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(endereco))
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Nenhum app de mapas encontrado", Toast.LENGTH_SHORT).show()
        }
    }
}