package com.example.cestacerta

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class CadastroMercadoActivity : AppCompatActivity() {
    private lateinit var etNomeEmpresa: EditText
    private lateinit var etCnpj: EditText
    private lateinit var etCep: EditText
    private lateinit var etRua: EditText
    private lateinit var etBairro: EditText
    private lateinit var etCidade: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro_mercado)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cadastro_mercado)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etNomeEmpresa = findViewById(R.id.etNomeEmpresa)
        etCnpj = findViewById(R.id.etCnpj)
        etCep = findViewById(R.id.etCep)
        etRua = findViewById(R.id.etRua)
        etBairro = findViewById(R.id.etBairro)
        etCidade = findViewById(R.id.etCidade)

        findViewById<LinearLayout>(R.id.cardMapa).setOnClickListener {
            Toast.makeText(this, "Mapa (em breve)", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnCadastrar).setOnClickListener {
            if (validarCampos()) {
                Toast.makeText(this, "Mercado cadastrado!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        configurarBarraInferior()
    }

    private fun validarCampos(): Boolean {
        val obrigatorios = listOf(
            etNomeEmpresa to "Informe o nome da empresa",
            etRua to "Informe a rua",
            etBairro to "Informe o bairro",
            etCidade to "Informe a cidade"
        )
        for ((campo, mensagem) in obrigatorios) {
            if (campo.text.toString().isBlank()) {
                campo.error = mensagem
                campo.requestFocus()
                return false
            }
        }

        val cnpj = etCnpj.text.toString().filter { it.isDigit() }
        if (cnpj.isNotEmpty() && cnpj.length != 14) {
            etCnpj.error = "CNPJ deve ter 14 números"
            etCnpj.requestFocus()
            return false
        }
        val cep = etCep.text.toString().filter { it.isDigit() }
        if (cep.isNotEmpty() && cep.length != 8) {
            etCep.error = "CEP deve ter 8 números"
            etCep.requestFocus()
            return false
        }
        return true
    }
}