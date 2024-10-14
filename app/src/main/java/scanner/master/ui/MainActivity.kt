package scanner.master.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import scanner.master.R
import scanner.master.databinding.ActivityMainBinding



class MainActivity : AppCompatActivity() {

    private val cameraPermissionRequestCode = 1
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.btnScan.setOnClickListener {
            startScanning()
        }


        // Trong file Activity của bạn
        val edtDomain = findViewById<EditText>(R.id.edtDomain)
        val edtDelayTimeAfterScanQr = findViewById<EditText>(R.id.edtDelayTimeAfterScanQr)
        val btnSetDomain = findViewById<Button>(R.id.btnSetDomain)

        val sharedPreferences = getSharedPreferences("EventNcbd", Context.MODE_PRIVATE)

// Đặt OnClickListener cho btnSetDomain
        btnSetDomain.setOnClickListener {
            val domain = edtDomain.text.toString()
            val edtDelayTimeAfterScanQr = edtDelayTimeAfterScanQr.text.toString()
            val editor = sharedPreferences.edit()
            editor.putString("domain", domain)
            editor.putString("edtDelayTimeAfterScanQr", edtDelayTimeAfterScanQr)
            editor.apply()
        }

        // Khi ứng dụng khởi động, lấy giá trị domain từ SharedPreferences và đặt vào EditText
        val domain = sharedPreferences.getString("domain", "")
        edtDomain.setText(domain)
        val timeDelay = sharedPreferences.getString("edtDelayTimeAfterScanQr", "")
        edtDomain.setText(timeDelay)

    }

    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraQrcode()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraQrcode()
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, cameraPermissionRequestCode)
            }
        }
    }

    private fun openCameraQrcode() {
        val intent = Intent(this, BarcodeScanningActivity::class.java)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraPermissionRequestCode) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCameraQrcode()
            }
        }
    }
}