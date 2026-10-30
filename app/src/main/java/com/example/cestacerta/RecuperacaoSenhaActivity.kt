package com.example.cestacerta

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class RecuperacaoSenhaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperacao_senha)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recuperacao_senha)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmailRecuperacao)

        findViewById<MaterialButton>(R.id.btnEnviarCodigo).setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "E-mail inválido"
                etEmail.requestFocus()
                return@setOnClickListener
            }
            Toast.makeText(this, "Código enviado para $email", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnVoltarInicio).setOnClickListener {
            finish()
        }
    }
}
