package com.example.cestacerta

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val btnCadastrar = findViewById<MaterialButton>(R.id.btnCadastrar)
        btnCadastrar.setOnClickListener {
            if (validarCampos()) cadastrar(btnCadastrar)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            supabase.auth.awaitInitialization()
            if (!estaLogado()) {
                Toast.makeText(this@CadastroMercadoActivity, "Entre na sua conta para cadastrar um mercado", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@CadastroMercadoActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun cadastrar(botao: MaterialButton) {
        val cnpj = etCnpj.text.toString().filter { it.isDigit() }.ifEmpty { null }
        val cep = etCep.text.toString().filter { it.isDigit() }
        val endereco = listOf(etRua, etBairro, etCidade)
            .map { it.text.toString().trim() }
            .plus(cep)
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        botao.isEnabled = false
        lifecycleScope.launch {
            try {
                supabase.auth.awaitInitialization()
                val usuario = supabase.auth.currentUserOrNull()
                if (usuario == null) {
                    mostrar("Entre com uma conta de vendedor para cadastrar um mercado")
                    return@launch
                }

                val local = buscarCoordenadas(endereco)
                if (local == null) {
                    mostrar("Endereço não encontrado. Confira os dados e tente novamente.")
                    return@launch
                }

                supabase.from("empresas").insert(
                    NovaEmpresa(
                        vendedorId = usuario.id,
                        nome = etNomeEmpresa.text.toString().trim(),
                        cnpj = cnpj,
                        endereco = endereco,
                        latitude = local.latitude,
                        longitude = local.longitude
                    )
                )
                mostrar("Mercado cadastrado!")
                finish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: PostgrestRestException) {
                mostrar(
                    when (e.code) {
                        "23505" -> "Este CNPJ já está cadastrado"
                        "42501" -> "Apenas contas de vendedor podem cadastrar mercados"
                        else -> "Não foi possível cadastrar o mercado. Tente novamente."
                    }
                )
            } catch (e: Exception) {
                mostrar("Erro de conexão. Tente novamente.")
            } finally {
                botao.isEnabled = true
            }
        }
    }

    @Suppress("DEPRECATION") // a versão nova de getFromLocationName só existe a partir do Android 13
    private suspend fun buscarCoordenadas(endereco: String): Address? =
        withContext(Dispatchers.IO) {
            Geocoder(this@CadastroMercadoActivity)
                .getFromLocationName(endereco, 1)
                ?.firstOrNull()
        }

    private fun mostrar(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
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