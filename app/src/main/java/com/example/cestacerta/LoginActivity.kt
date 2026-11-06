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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            if (validarCampos()) entrar(btnLogin)
        }

        findViewById<TextView>(R.id.tvEsqueciSenha).setOnClickListener {
            startActivity(Intent(this, RecuperacaoSenhaActivity::class.java))
        }

        findViewById<ImageView>(R.id.ivSocialApps).setOnClickListener {
            Toast.makeText(this, "Login social (em breve)", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvCadastro).setOnClickListener {
            startActivity(Intent(this, CadastroUsuarioActivity::class.java))
        }
    }

    private fun entrar(botao: MaterialButton) {
        val emailDigitado = etEmail.text.toString().trim()
        val senhaDigitada = etSenha.text.toString()

        botao.isEnabled = false
        lifecycleScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    email = emailDigitado
                    password = senhaDigitada
                }
                startActivity(Intent(this@LoginActivity, InicioActivity::class.java))
                finish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthRestException) {
                val mensagem = when (e.errorCode) {
                    AuthErrorCode.InvalidCredentials -> "E-mail ou senha incorretos"
                    AuthErrorCode.EmailNotConfirmed -> "Confirme seu e-mail antes de entrar"
                    else -> "Não foi possível entrar. Tente novamente."
                }
                Toast.makeText(this@LoginActivity, mensagem, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Erro de conexão. Tente novamente.", Toast.LENGTH_SHORT).show()
            } finally {
                botao.isEnabled = true
            }
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