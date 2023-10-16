package com.appbtbalanca

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class dados_contato : AppCompatActivity() {

    companion object {
        private const val REQUEST_SEND_EMAIL = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dados_contato)

        val editTextNome = findViewById<EditText>(R.id.editTextDadosNome)
        val editTextEmail = findViewById<EditText>(R.id.editTextDadosEmail)
        val editTextTelefone = findViewById<EditText>(R.id.editTextDadosTelefone)
        val buttonProximo = findViewById<Button>(R.id.buttonDadosProximo)

        editTextTelefone.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length == 2) {
                    editTextTelefone.setText("($s)")
                    editTextTelefone.setSelection(s.length + 2)
                }
            }
        })

        buttonProximo.setOnClickListener {
            val nome = editTextNome.text.toString()
            val email = editTextEmail.text.toString()
            val telefone = editTextTelefone.text.toString()

            if (nome.isBlank() || email.isBlank() || telefone.isBlank() ||
                !email.contains("@") || telefone.length != 13) {
                Toast.makeText(this, "Por favor, preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
            } else {
                sendEmail(nome, email, telefone)
            }
        }
    }

    private fun sendEmail(nome: String, email: String, telefone: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc822"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("seuemail@dominio.com", "outroemail@dominio.com"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Dados do Aplicativo")
        intent.putExtra(Intent.EXTRA_TEXT, "Nome: $nome\nE-mail: $email\nTelefone: $telefone")

        try {
            startActivityForResult(Intent.createChooser(intent, "Escolha um aplicativo de e-mail:"), REQUEST_SEND_EMAIL)
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum aplicativo de e-mail encontrado!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SEND_EMAIL) {
            val nextActivityIntent = Intent(this, dados_bluetooth_c20::class.java)
            startActivity(nextActivityIntent)
            finish()
        }
    }
}
