package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificacoesActivity : AppCompatActivity() {
    data class Notificacao(val mercado: String, val logo: Int, val mensagem: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notificacoes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notificacoes)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val notificacoes = listOf(
            Notificacao("Viva Fresco", R.drawable.mercado_viva_fresco, "Desconto de 20% em Laticínios"),
            Notificacao("Mercado Central", R.drawable.mercado_central, "Desconto de 5% em Bebidas"),
            Notificacao("Hipermais", R.drawable.mercado_hipermais, "Desconto de 50% em Doces"),
            Notificacao("Hipermais", R.drawable.mercado_hipermais, "Desconto de 2% em Salgadinhos"),
            Notificacao("Viva Fresco", R.drawable.mercado_viva_fresco, "Desconto de 80% em Frios")
        )

        val lista = findViewById<LinearLayout>(R.id.listaNotificacoes)
        for (notificacao in notificacoes) {
            val cartao = layoutInflater.inflate(R.layout.item_notificacao, lista, false)
            cartao.findViewById<ImageView>(R.id.ivLogoMercado).setImageResource(notificacao.logo)
            cartao.findViewById<TextView>(R.id.tvNomeMercado).text = notificacao.mercado
            cartao.findViewById<TextView>(R.id.tvMensagem).text = notificacao.mensagem

            cartao.setOnClickListener {
                val abrir = Intent(this, MercadoEspecificoActivity::class.java)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_NOME, notificacao.mercado)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_LOGO, notificacao.logo)
                abrir.putExtra(MercadoEspecificoActivity.EXTRA_ENDERECO, notificacao.mercado)
                startActivity(abrir)
            }
            lista.addView(cartao)
        }

        configurarBarraInferior()
    }
}
