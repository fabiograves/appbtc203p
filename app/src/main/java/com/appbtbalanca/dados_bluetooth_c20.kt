package com.appbtbalanca

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*
import java.util.LinkedList


// 13 0526053840
@SuppressLint("MissingPermission", "SetTextI18n")
class dados_bluetooth_c20 : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null

    private lateinit var logTextView: TextView
    private lateinit var macIdEditText: EditText
    private lateinit var macSerialEditText: EditText
    private lateinit var connectBluetoothButton: Button
    private lateinit var textViewDadosC20: TextView
    private var lastDisplayedValue: String? = null


    companion object {
        const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        const val REQUEST_ENABLE_BT = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dados_bluetooth_c20)

        logTextView = findViewById(R.id.textViewLog)
        macIdEditText = findViewById(R.id.editTextIdMac)
        macSerialEditText = findViewById(R.id.editTextSerialMac)
        connectBluetoothButton = findViewById(R.id.buttonConectarBt)
        textViewDadosC20 = findViewById(R.id.textViewDadosC20)

        // Recuperar os valores salvos nas preferências e setá-los nos campos de texto
        val (savedId, savedSerial) = retrieveFromPreferences()
        macIdEditText.setText(savedId)
        macSerialEditText.setText(savedSerial)

        initializeBluetooth()

        connectBluetoothButton.setOnClickListener {
            val macID = macIdEditText.text.toString()
            val macSerial = macSerialEditText.text.toString()

            // Salva os dados digitados nas preferências
            saveToPreferences(macID, macSerial)

            try {
                val macAddress = decodeMac(macID, macSerial)
                toggleBluetoothConnection(macAddress)
            } catch (e: Exception) {
                logTextView.text = "ID ou Serial digitado errado."
            }
        }
    }

    private fun codificarMac(mac: String): Pair<String, String> {
        val bytes = mac.split(":")
        val id = bytes[2]
        val constValue = 5 // Valor constante para adicionar/subtrair

        val serial = bytes.filterIndexed { index, _ -> index != 2 }
            .map { (it.toInt(16) + constValue).and(0xFF).toString(16).padStart(2, '0')
                .uppercase(Locale.getDefault()) }
            .joinToString("")

        return Pair(id, serial)
    }


    @Throws(Exception::class)
    private fun decodeMac(id: String, serial: String): String {
        val serialBytes: List<String> = if (serial.contains(":")) {
            serial.split(":")
        } else {
            serial.chunked(2)
        }

        val constValue = 5

        val decodedBytes = mutableListOf<String>()
        decodedBytes.add((serialBytes[0].toInt(16) - constValue).and(0xFF).toString(16).padStart(2, '0').toUpperCase())
        decodedBytes.add((serialBytes[1].toInt(16) - constValue).and(0xFF).toString(16).padStart(2, '0').toUpperCase())
        decodedBytes.add(id)
        decodedBytes.add((serialBytes[2].toInt(16) - constValue).and(0xFF).toString(16).padStart(2, '0').toUpperCase())
        decodedBytes.add((serialBytes[3].toInt(16) - constValue).and(0xFF).toString(16).padStart(2, '0').toUpperCase())
        decodedBytes.add((serialBytes[4].toInt(16) - constValue).and(0xFF).toString(16).padStart(2, '0').toUpperCase())

        return decodedBytes.joinToString(":")
    }


    private fun saveToPreferences(id: String, serial: String) {
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("ID", id)
        editor.putString("SERIAL", serial)
        editor.apply()
    }

    private fun retrieveFromPreferences(): Pair<String?, String?> {
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val savedId = sharedPref.getString("ID", "")
        val savedSerial = sharedPref.getString("SERIAL", "")
        return Pair(savedId, savedSerial)
    }



    private fun toggleBluetoothConnection(macAddress: String) {
        val devices = bluetoothAdapter?.bondedDevices

        if (devices == null || devices.isEmpty()) {
            logTextView.text = "Não foi encontrado nenhum dispositivo Bluetooth"
            return
        }

        bluetoothSocket?.let { socket ->
            if (socket.isConnected) {
                disconnectBluetooth()
                bluetoothSocket = null  // Set the socket to null after disconnecting
                return
            }
        }

        val selectedDevice = devices.find { it.address == macAddress }

        if (selectedDevice == null) {
            logTextView.text = "ID ou Serial errados."
            return
        }

        try {
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()

            startReadingDataFromBluetooth()

            connectBluetoothButton.text = "Desconectar"
            logTextView.text = "Conectado."
        } catch (e: IOException) {
            e.printStackTrace()
            logTextView.text = "Não foi possível se conectar."
        }
    }



    private fun startReadingDataFromBluetooth() {
        Thread {
            bluetoothSocket?.inputStream?.let { inputStream ->
                val buffer = ByteArray(1024)
                val receivedValues = LinkedList<String>()
                var readCounter = 0 // contador
                var lastReadTime = System.currentTimeMillis()

                while (true) {
                    try {
                        val bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            lastReadTime = System.currentTimeMillis() // Atualiza o último tempo de leitura

                            var data = String(buffer, 0, bytes).trim()

                            // Remove remove os caracteres indesejados
                            data = data.replace(Regex("[^0-9.]"), "")

                            // Se o ultimo for ponto remove
                            if (data.endsWith(".")) {
                                data = data.dropLast(1)
                            }

                            receivedValues.add(data)
                            if (receivedValues.size > 20) {
                                receivedValues.removeFirst()
                            }

                            val mostFrequentValue = receivedValues.groupingBy { it }
                                .eachCount()
                                .maxByOrNull { it.value }?.key

                            // Verificar se o valor mais frequente é diferente do último valor exibido
                            if (mostFrequentValue != lastDisplayedValue) {
                                runOnUiThread {
                                    textViewDadosC20.text = mostFrequentValue ?: ""
                                }
                                lastDisplayedValue = mostFrequentValue
                            }

                            readCounter++ // incrementar contador
                            if (readCounter >= 500) {
                                receivedValues.clear()
                                readCounter = 0
                            }
                        }

                        // Verifica se passaram 3 segundos desde a última leitura
                        if (System.currentTimeMillis() - lastReadTime > 3000 || !isSocketConnected(bluetoothSocket)) {
                            runOnUiThread {
                                disconnectBluetooth()
                                textViewDadosC20.text = lastDisplayedValue ?: "" // Use o último valor exibido aqui
                                logTextView.text = "Conexão perdida"
                            }
                            return@Thread // Sai do thread
                        }

                        Thread.sleep(10) // Pausa pra n dar over na cpu
                    } catch (e: IOException) {
                        e.printStackTrace()
                        runOnUiThread {
                            disconnectBluetooth()
                            textViewDadosC20.text = ""
                            logTextView.text = "Conexão perdida"
                        }
                        return@Thread
                    }
                }
            }
        }.start()
    }


    fun isSocketConnected(socket: BluetoothSocket?): Boolean {
        try {
            val isConnectedMethod = BluetoothSocket::class.java.getDeclaredMethod("isConnected")
            return isConnectedMethod.invoke(socket) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun disconnectBluetooth() {
        try {
            bluetoothSocket?.close()
            connectBluetoothButton.text = "Conectar"
            logTextView.text = "Bluetooth desconectado"
            textViewDadosC20.text = ""
        } catch (e: IOException) {
            logTextView.text = "Erro ao desconectar o Bluetooth"
        }
    }

    private fun initializeBluetooth() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            logTextView.text = "Dispositivo não suporta Bluetooth."
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun checkBluetoothPermissions() =
        ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
            REQUEST_BLUETOOTH_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth()
            } else {
                logTextView.text = "Permissão para Bluetooth negada!"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    logTextView.text = "Bluetooth ativado!"
                } else {
                    logTextView.text = "Falha ao ativar Bluetooth!"
                }
            }
        }
    }
}
