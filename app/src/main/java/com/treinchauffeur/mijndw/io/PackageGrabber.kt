package com.treinchauffeur.mijndw.io

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.treinchauffeur.mijndw.AboutActivity
import java.io.File
import java.util.Objects

class PackageGrabber

/**
 * This class is used to retrieve certain apk files.
 *
 * @param context
 */
    (var context: Context, var activity: AboutActivity) {
    var packagesToGrab = arrayOf("nl.ns.wissel" /*, "nl.ns.timtim"*/)
    fun requestFolderPerms() {}
    /*fun writeFiles(uri: Uri) {
        try {
            for (s in packagesToGrab) {
                val pm = context.packageManager
                val packageInfo = pm.getPackageInfo("nl.ns.wissel", 0)
                val version = packageInfo.longVersionCode
                val apkPath = packageInfo.applicationInfo.sourceDir


                val file = File(apkPath)
                Log.d(TAG, "onLongClick: " + file.length())
                val out = context.contentResolver.openOutputStream(Objects.requireNonNull(uri))!!
                out.write(file.readBytes())
                out.close()

                Toast.makeText(context, "Succesvol geëxporteerd!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeFiles: ", e)
        }
    }*/

    companion object {
        const val PACKAGE_REQUEST_CODE = 1312
        const val TAG = "PackageGrabber"
    }
}