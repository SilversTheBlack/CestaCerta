package com.example.cestacerta

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
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
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class Perfil(val nome: String, val tipo: String)

private const val BUCKET_AVATARES = "avatares"
private const val TAMANHO_FOTO = 512

private fun caminhoFoto(usuarioId: String) = "$usuarioId/avatar.jpg"

class PerfilActivity : AppCompatActivity() {
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var tvNome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvTipoPerfil: TextView
    private var dadosCarregados = false

    private val escolherFoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) enviarFoto(uri)
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

        tvNome = findViewById(R.id.tvNome)
        tvEmail = findViewById(R.id.tvEmail)
        tvTipoPerfil = findViewById(R.id.tvTipoPerfil)

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
            val usuario = supabase.auth.currentUserOrNull()
            if (usuario == null) {
                startActivity(Intent(this@PerfilActivity, LoginActivity::class.java))
                finish()
                return@launch
            }
            if (!dadosCarregados) {
                dadosCarregados = true
                launch { carregarFoto(usuario.id) }
                carregarPerfil(usuario)
            }
        }
    }

    private suspend fun carregarPerfil(usuario: UserInfo) {
        tvEmail.text = usuario.email
        try {
            val perfil = supabase.from("perfis")
                .select(Columns.list("nome", "tipo")) {
                    filter { eq("id", usuario.id) }
                }
                .decodeSingle<Perfil>()
            tvNome.text = perfil.nome
            tvTipoPerfil.text = if (perfil.tipo == "vendedor") "Vendedor" else "Cliente"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível carregar seu perfil.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun carregarFoto(usuarioId: String) {
        try {
            val bytes = supabase.storage.from(BUCKET_AVATARES).downloadAuthenticated(caminhoFoto(usuarioId))
            val bitmap = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            if (bitmap != null) mostrarFoto(bitmap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Usuário ainda não tem foto (ou sem conexão): mantém o ícone padrão
        }
    }

    private fun enviarFoto(uri: Uri) {
        val usuario = supabase.auth.currentUserOrNull() ?: return
        ivFotoPerfil.isEnabled = false
        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { prepararFoto(uri) }
                supabase.storage.from(BUCKET_AVATARES).upload(caminhoFoto(usuario.id), bytes) {
                    upsert = true
                    contentType = ContentType.Image.JPEG
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { mostrarFoto(it) }
                Toast.makeText(this@PerfilActivity, "Foto atualizada!", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@PerfilActivity, "Não foi possível salvar a foto.", Toast.LENGTH_SHORT).show()
            } finally {
                ivFotoPerfil.isEnabled = true
            }
        }
    }

    private fun mostrarFoto(bitmap: Bitmap) {
        ivFotoPerfil.setPadding(0, 0, 0, 0)
        ivFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
        ivFotoPerfil.setImageBitmap(bitmap)
    }

    // Reduz a imagem (lado menor = 512px), corrige a rotação da câmera e comprime em JPEG
    private fun prepararFoto(uri: Uri): ByteArray {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }

        var amostra = 1
        while (limites.outWidth / (amostra * 2) >= TAMANHO_FOTO &&
            limites.outHeight / (amostra * 2) >= TAMANHO_FOTO
        ) amostra *= 2

        val original = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra })
        } ?: throw IOException("Imagem inválida")

        val orientacao = contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val rotacao = when (orientacao) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val escala = TAMANHO_FOTO.toFloat() / minOf(original.width, original.height)
        val matriz = Matrix().apply {
            if (escala < 1f) postScale(escala, escala)
            postRotate(rotacao)
        }
        val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)

        return ByteArrayOutputStream().use { saida ->
            final.compress(Bitmap.CompressFormat.JPEG, 85, saida)
            saida.toByteArray()
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