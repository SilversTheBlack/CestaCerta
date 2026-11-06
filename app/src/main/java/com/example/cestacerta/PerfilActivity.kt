package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PerfilActivity : AppCompatActivity() {
    private lateinit var ivFotoPerfil: ImageView

    private val escolherFoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                ivFotoPerfil.setPadding(0, 0, 0, 0)
                ivFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
                ivFotoPerfil.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.perfil)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        ivFotoPerfil.clipToOutline = true

        findViewById<TextView>(R.id.tvNome).text = "Rogério Gomes Cunha"
        findViewById<TextView>(R.id.tvEmail).text = "Rogerio.Gomes@outlook.com"
        findViewById<TextView>(R.id.tvTipoPerfil).text = "Cliente"

        ivFotoPerfil.setOnClickListener {
            escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        findViewById<TextView>(R.id.tvRedefinirSenha).setOnClickListener {
            startActivity(Intent(this, RecuperacaoSenhaActivity::class.java))
        }

        val btnSair = findViewById<MaterialButton>(R.id.btnSair)
        btnSair.setOnClickListener { sair(btnSair) }

        configurarBarraInferior()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            supabase.auth.awaitInitialization()
            if (!estaLogado()) {
                startActivity(Intent(this@PerfilActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun sair(botao: MaterialButton) {
        botao.isEnabled = false
        lifecycleScope.launch {
            try {
                supabase.auth.signOut()
                val abrir = Intent(this@PerfilActivity, InicioActivity::class.java)
                abrir.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(abrir)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@PerfilActivity, "Não foi possível sair. Tente novamente.", Toast.LENGTH_SHORT).show()
            } finally {
                botao.isEnabled = true
            }
        }
    }
}
