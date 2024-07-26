package com.example.afrozkeliyewebview

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.util.Date

class JavaScriptInterface(private val context: Context) {
    companion object {
        private var fileMimeType: String? = null

        @JvmStatic
        fun getBase64StringFromBlobUrl(blobUrl: String, mimeType: String): String {
            return if (blobUrl.startsWith("blob")) {
                fileMimeType = mimeType
                "javascript: var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '$blobUrl', true);" +
                        "xhr.setRequestHeader('Content-type','$mimeType;charset=UTF-8');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "    if (this.status == 200) {" +
                        "        var blobFile = this.response;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blobFile);" +
                        "        reader.onloadend = function() {" +
                        "            var base64data = reader.result;" +
                        "            Android.getBase64FromBlobData(base64data);" +
                        "        };" +
                        "    }" +
                        "};" +
                        "xhr.send();"
            } else {
                "javascript: console.log('It is not a Blob URL');"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @JavascriptInterface
    @Throws(IOException::class)
    fun getBase64FromBlobData(base64Data: String) {
        convertBase64StringToFileAndStoreIt(base64Data)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(IOException::class)
    private fun convertBase64StringToFileAndStoreIt(base64Data: String) {
        val notificationId = 1
        val currentDateTime = DateFormat.getDateTimeInstance().format(Date())
        val newTime = currentDateTime.replaceFirst(", ".toRegex(), "_")
            .replace(" ".toRegex(), "_")
            .replace(":".toRegex(), "-")
        Log.d("fileMimeType ====> ", fileMimeType.toString())
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = mimeTypeMap.getExtensionFromMimeType(fileMimeType)
        val dwldsPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                    + "/" + newTime + "_." + extension
        )
        val regex = "^data:$fileMimeType;base64,"
        val dataAsBytes = Base64.decode(base64Data.replaceFirst(regex.toRegex(), ""), Base64.DEFAULT)
        try {
            FileOutputStream(dwldsPath).use { os ->
                os.write(dataAsBytes)
                os.flush()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "FAILED TO DOWNLOAD THE FILE!", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        if (dwldsPath.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            val apkURI = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                dwldsPath
            )
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val CHANNEL_ID = "MYCHANNEL"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW)
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setContentText("You have got something new!")
                .setContentTitle("File downloaded")
                .setContentIntent(pendingIntent)
                .setChannelId(CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .build()
            notificationManager.createNotificationChannel(notificationChannel)
            notificationManager.notify(notificationId, notification)
        }
        Toast.makeText(context, "FILE DOWNLOADED!", Toast.LENGTH_SHORT).show()
    }
}