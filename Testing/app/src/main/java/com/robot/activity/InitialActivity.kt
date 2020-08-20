package com.robot.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.robot.R
import com.robot.ui.login.IpAddressFragment
import com.robot.vpn.VPNManager
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class InitialActivity : AppCompatActivity() {

    companion object {
        const val IMPORT_FILE_REQUEST = 1024
        lateinit var self: InitialActivity
    }

    var onProfilePicked: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        if (savedInstanceState == null) {
            val fragment = IpAddressFragment.newInstance()
            fragment.onDidConnect = {
                val intent = Intent(this, RobotActivity::class.java)
                startActivity(intent)
                finish()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
        }


        self = this
    }

    override fun onStart() {
        super.onStart()

        checkPermissions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_FILE_REQUEST && resultCode == RESULT_OK) {
            val selectedFile = data?.data //The uri with the location of the file
            val name = selectedFile?.let { getFileName(it) }

            if (selectedFile != null && name?.contains(".ovpn") == true) {
                val text = readText(selectedFile)
                if (text.isNullOrEmpty()) {
                    displayAlert(getString(R.string.incorrect_file_title), getString(R.string.incorrect_file_text))
                } else {
                    //copy file
                    val destination = VPNManager.instance.getVPNProfileFile()

                    val contentDescriber: Uri = selectedFile
                    var `in`: InputStream? = null
                    var out: OutputStream? = null
                    try {
                        `in` = contentResolver.openInputStream(contentDescriber)
                        out = FileOutputStream(destination)
                        val buffer = ByteArray(1024)
                        var len = 0
                        while (`in`?.read(buffer).also {
                                if (it != null) {
                                    len = it
                                }
                            } != -1) {
                            out.write(buffer, 0, len)
                        }
                    } finally {
                        `in`?.close()
                        out?.close()
                    }

                   onProfilePicked?.invoke()
                }
            } else {
                displayAlert(getString(R.string.incorrect_file_title), getString(R.string.incorrect_file_text))
            }
        }
    }

    private fun readText(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val string = inputStream?.bufferedReader().use { it?.readText() }
            string
        } catch (e: Exception) {
            null
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun getFileName(uri: Uri): String? {
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            if (cursor.count <= 0) {
                cursor?.close()
                throw IllegalArgumentException("Can't obtain file name, cursor is empty")
            }
        }
        cursor?.moveToFirst()
        val fileName: String? =
            cursor?.getString(cursor?.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        cursor?.close()
        return fileName
    }

    private fun checkPermissions()
    {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {}

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener {
            }
            .check()
    }

    fun displayAlert(title:String?, message:String?) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
        }

        alertDialog.show()
    }
}