package com.pixesoj.freefirelike.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.config.AppConfig
import com.pixesoj.freefirelike.utils.HelperUtils
import java.io.File

@SuppressLint("UseSwitchCompatOrMaterialCode")
class SettingsActivity  : AppCompatActivity()  {
    private var context: Context? = null
    private var activity: Activity? = null
    private var switchButtonNotifications: Switch? = null
    private var linearLayoutActionBar: LinearLayout? = null
    private var imageViewActionBarBack: ImageView? = null
    private var relativeLayoutDeletedCache: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_settings)
        initValues()
        initEvents()
    }

    private fun initValues() {
        context = this
        activity = this

        switchButtonNotifications = findViewById<Switch>(R.id.switchButtonNotifications)
        linearLayoutActionBar = findViewById<LinearLayout>(R.id.linearLayoutActionBar)
        imageViewActionBarBack = findViewById<ImageView>(R.id.imageViewActionBarBack)
        relativeLayoutDeletedCache = findViewById<RelativeLayout>(R.id.relativeLayoutDeletedCache)
    }

    private fun initEvents() {
        activity?.let {
            val statusBarHeight = HelperUtils.getStatusBarHeight(it)
            linearLayoutActionBar?.setPadding(0, statusBarHeight, 0, 0)
        }

        imageViewActionBarBack?.setOnClickListener { finish() }

        switchButtonNotifications?.setChecked(AppConfig.isEnabledNotifications())
        switchButtonNotifications?.setOnCheckedChangeListener { b, isChecked ->
            AppConfig.setEnabledNotifications(isChecked)
        }



        relativeLayoutDeletedCache?.setOnClickListener({ v ->
            try {
                deleteCache(applicationContext)
                Toast.makeText(context, "¡Se ha eliminado el caché de la aplicación!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { }
        })

    }

    fun deleteCache(context: Context) {
        try {
            val dir = context.getCacheDir()
            deleteDir(dir)
        } catch (_: Exception) { }
    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory()) {
            val children = dir.list()
            for (child in children!!) {
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile()) {
            return dir.delete()
        } else {
            return false
        }
    }
}