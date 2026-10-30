package com.example.cestacerta

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.configurarBarraInferior() {
    findViewById<ImageButton>(R.id.btnInicio).setOnClickListener { abrirTela(InicioActivity::class.java) }
    findViewById<ImageButton>(R.id.btnNotificacoes).setOnClickListener { abrirTela(NotificacoesActivity::class.java) }
    findViewById<ImageButton>(R.id.btnSobre).setOnClickListener { abrirTela(SobreNosActivity::class.java) }
    findViewById<ImageButton>(R.id.btnPerfil).setOnClickListener { abrirTela(PerfilActivity::class.java) }
}

private fun AppCompatActivity.abrirTela(destino: Class<out Activity>) {
    if (this::class.java == destino) return
    val intent = Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    startActivity(intent)
}
