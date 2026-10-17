package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SobreNosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sobre_nos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sobre_nos)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnInicio).setOnClickListener {
            startActivity(Intent(this, InicioActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.btnNotificacoes).setOnClickListener {
            Toast.makeText(this, "Notificações (em breve)", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnSobre).setOnClickListener {
        }

        findViewById<ImageButton>(R.id.btnPerfil).setOnClickListener {
            Toast.makeText(this, "Perfil (em breve)", Toast.LENGTH_SHORT).show()
        }
    }
}