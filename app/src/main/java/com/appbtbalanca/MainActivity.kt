package com.appbtbalanca

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    // Defina uma constante para a chave das SharedPreferences
    private val FIRST_RUN = "firstRun"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences: SharedPreferences = getSharedPreferences("com.appbtbalanca", MODE_PRIVATE)

        // Utilize o Handler para criar um delay
        Handler().postDelayed({
            // Verifique se é a primeira execução do aplicativo
            if (sharedPreferences.getBoolean(FIRST_RUN, true)) {
                // Se for a primeira execução, mude para dados_contato Activity
                startActivity(Intent(this, dados_contato::class.java))

                // Marque o aplicativo como já executado
                sharedPreferences.edit().putBoolean(FIRST_RUN, false).apply()
            } else {
                // Se não for a primeira execução, vá para dados_bluetooth_c20 Activity
                startActivity(Intent(this, dados_bluetooth_c20::class.java))
            }

            // Finalize a MainActivity para que o usuário não volte para ela ao pressionar o botão "voltar"
            finish()
        }, 2000) // 3 segundos de delay
    }
}
