package com.example.cestacerta

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

fun AppCompatActivity.configurarBarraInferior() {
    findViewById<ImageButton>(R.id.btnInicio).setOnClickListener { abrirTela(InicioActivity::class.java) }
    findViewById<ImageButton>(R.id.btnNotificacoes).setOnClickListener { abrirTela(NotificacoesActivity::class.java) }
    findViewById<ImageButton>(R.id.btnSobre).setOnClickListener { abrirTela(SobreNosActivity::class.java) }
    findViewById<ImageButton>(R.id.btnPerfil).setOnClickListener {
        lifecycleScope.launch {
            supabase.auth.awaitInitialization()
            if (estaLogado()) abrirTela(PerfilActivity::class.java) else abrirTela(LoginActivity::class.java)
        }
    }
}

fun estaLogado(): Boolean = supabase.auth.currentSessionOrNull() != null

fun AppCompatActivity.exigirLogin(acao: () -> Unit) {
    lifecycleScope.launch {
        supabase.auth.awaitInitialization()
        if (estaLogado()) {
            acao()
        } else {
            Toast.makeText(this@exigirLogin, "Entre na sua conta para fazer isso", Toast.LENGTH_SHORT).show()
            abrirTela(LoginActivity::class.java)
        }
    }
}

private fun AppCompatActivity.abrirTela(destino: Class<out Activity>) {
    if (this::class.java == destino) return
    val intent = Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    startActivity(intent)
}
