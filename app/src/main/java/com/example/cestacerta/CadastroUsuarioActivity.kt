package com.example.cestacerta

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class CadastroUsuarioActivity : AppCompatActivity() {

    private lateinit var ivFotoPerfil: ImageView
    private lateinit var etNome: EditText
    private lateinit var etEmail: EditText
    private lateinit var etSenha: EditText
    private lateinit var spPerfil: Spinner

    private var fotoPerfil: Uri? = null

    private val escolherFoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                fotoPerfil = uri
                ivFotoPerfil.setPadding(0, 0, 0, 0)
                ivFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
                ivFotoPerfil.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro_usuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cadastro_usuario)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        etNome = findViewById(R.id.etNome)
        etEmail = findViewById(R.id.etEmail)
        etSenha = findViewById(R.id.etSenha)
        spPerfil = findViewById(R.id.spPerfil)

        val adapter = ArrayAdapter.createFromResource(this, R.array.perfis, R.layout.item_spinner)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        spPerfil.adapter = adapter

        ivFotoPerfil.setOnClickListener {
            escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        findViewById<MaterialButton>(R.id.btnAcessar).setOnClickListener {
            if (validarCampos()) {
                Toast.makeText(this, "Cadastro realizado!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun validarCampos(): Boolean {
        val nome = etNome.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString()

        if (nome.isEmpty()) {
            etNome.error = "Informe seu nome"
            etNome.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "E-mail inválido"
            etEmail.requestFocus()
            return false
        }
        if (!senhaForte(senha)) {
            etSenha.error = "A senha não atende aos requisitos"
            etSenha.requestFocus()
            return false
        }
        if (spPerfil.selectedItemPosition == 0) {
            Toast.makeText(this, "Selecione seu tipo de perfil", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun senhaForte(senha: String): Boolean {
        return senha.length >= 8 &&
                senha.count { it.isDigit() } >= 2 &&
                senha.any { !it.isLetterOrDigit() } &&
                senha.any { it.isUpperCase() } &&
                senha.any { it.isLowerCase() }
    }
}