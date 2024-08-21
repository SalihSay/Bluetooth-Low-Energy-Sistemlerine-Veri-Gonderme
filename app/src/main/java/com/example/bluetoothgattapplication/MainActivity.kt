package com.example.bluetoothgattapplication

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetoothgattapplication.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    private val deviceAddress = "ED:36:25:4D:81:7F" // Mikroişlemcinin gerçek MAC adresi
    private val serviceUUID = "00003670-0000-1000-8000-00805F9B34FB" // Servis UUID
    private val characteristicUUID = "00002902-0000-1000-8000-00805F9B34FB" // Karakteristik UUID

    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), BLUETOOTH_PERMISSION_REQUEST_CODE)
        } else {
            connectToDevice()
        }

        binding.button1.setOnClickListener { sendData(0x01) }
        binding.button2.setOnClickListener { sendData(0x02) }
        binding.button4.setOnClickListener { sendData(0x04) }
        binding.button8.setOnClickListener { sendData(0x08) }
    }

    private fun connectToDevice() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Cihaza bağlandı, servisleri keşfediyoruz...")
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Toast.makeText(this@MainActivity, "Bağlantı kesildi!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt?.services
                services?.forEach { service ->
                    val serviceUuid = service.uuid.toString()
                    Log.d("BLE", "Service UUID: $serviceUuid")

                    val characteristics = service.characteristics
                    characteristics.forEach { characteristic ->
                        val characteristicUuid = characteristic.uuid.toString()
                        Log.d("BLE", "Characteristic UUID: $characteristicUuid")
                        // Örnek karakteristiği burada ayarlıyoruz
                        if (characteristicUuid == characteristicUUID) {
                            this@MainActivity.characteristic = characteristic
                            Log.d("BLE", "Karakteristik bulundu ve ayarlandı!")
                        }
                    }
                }
            } else {
                Log.d("BLE", "Servisler keşfedilemedi, durum: $status")
                Toast.makeText(this@MainActivity, "Servisler keşfedilemedi!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Toast.makeText(this@MainActivity, "Veri gönderildi!", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("BLE", "Veri gönderimi başarısız, durum: $status")
                Toast.makeText(this@MainActivity, "Veri gönderimi başarısız!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendData(data: Int) {
        characteristic?.let {
            it.value = byteArrayOf(data.toByte())
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            if (it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                bluetoothGatt?.writeCharacteristic(it)
            } else {
                Toast.makeText(this, "Karakteristik yazma özelliğine sahip değil!", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Özellik bulunamadı!", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToDevice()
        } else {
            Toast.makeText(this, "Bluetooth izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothGatt?.close()
    }
}
