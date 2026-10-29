package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etSenha: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etEmail = findViewById(R.id.etEmail)
        etSenha = findViewById(R.id.etSenha)

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            if (validarCampos()) {
                // TODO: autenticar o usuário (banco de dados / API)
                Toast.makeText(this, "E-mail e senha válidos!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, InicioActivity::class.java))
                finish()
            }
        }

        findViewById<TextView>(R.id.tvEsqueciSenha).setOnClickListener {
            Toast.makeText(this, "Recuperar senha (em breve)", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.ivSocialApps).setOnClickListener {
            Toast.makeText(this, "Login social (em breve)", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvCadastro).setOnClickListener {
            startActivity(Intent(this, CadastroUsuarioActivity::class.java))
        }
    }

    private fun validarCampos(): Boolean {
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "E-mail inválido"
            etEmail.requestFocus()
            return false
        }
        if (senha.isEmpty()) {
            etSenha.error = "Informe sua senha"
            etSenha.requestFocus()
            return false
        }
        return true
    }
}