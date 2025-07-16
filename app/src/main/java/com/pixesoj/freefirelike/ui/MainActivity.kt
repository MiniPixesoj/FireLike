package com.pixesoj.freefirelike.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.airbnb.lottie.LottieAnimationView
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.builder.TextSpanBuilder
import com.pixesoj.freefirelike.config.AppConfig
import com.pixesoj.freefirelike.manager.AccountManager
import com.pixesoj.freefirelike.manager.CustomDialog
import com.pixesoj.freefirelike.ui.fragments.BanFragment
import com.pixesoj.freefirelike.ui.fragments.InfoFragment
import com.pixesoj.freefirelike.ui.fragments.LikesFragment
import com.pixesoj.freefirelike.ui.fragments.MenuFragment
import com.pixesoj.freefirelike.ui.fragments.TopsFragment
import com.pixesoj.freefirelike.utils.AppUtils
import es.dmoral.toasty.Toasty
import nl.joery.animatedbottombar.AnimatedBottomBar
import nl.joery.animatedbottombar.AnimatedBottomBar.OnTabSelectListener
import androidx.core.net.toUri
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private var topsFragment: TopsFragment? = null
    private var likesFragment: LikesFragment? = null
    private var infoFragment: InfoFragment? = null
    private var banFragment: BanFragment? = null
    private var menuFragment: MenuFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val rootView = findViewById<View>(R.id.root_view) // tu layout principal
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val bottomBar = findViewById<AnimatedBottomBar>(R.id.bottom_bar)
        val vpHorizontalNtb = findViewById<ViewPager>(R.id.vp_horizontal_ntb)
        val adapter = ViewPagerAdapter(supportFragmentManager)

        topsFragment = TopsFragment()
        likesFragment = LikesFragment()
        infoFragment = InfoFragment()
        banFragment = BanFragment()
        menuFragment = MenuFragment()

        adapter.addFragment(infoFragment!!)
        adapter.addFragment(likesFragment!!)
        adapter.addFragment(banFragment!!)
        adapter.addFragment(menuFragment!!)

        vpHorizontalNtb.adapter = adapter
        vpHorizontalNtb.currentItem = 0
        bottomBar.selectTabAt(0, false)

        vpHorizontalNtb.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                bottomBar.selectTabAt(position, true)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        bottomBar.setOnTabSelectListener(object : OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                vpHorizontalNtb.currentItem = newIndex
            }

            override fun onTabReselected(index: Int, tab: AnimatedBottomBar.Tab) {}
        })
        vpHorizontalNtb.offscreenPageLimit = 4
        AccountManager.selectAccount(null)
        checkUpdate()
    }

    class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val mFragmentList = mutableListOf<Fragment>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment) {
            mFragmentList.add(fragment)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun checkUpdate() {
        if (AppConfig.isUpdateAvailable()) {
            var customDialog: CustomDialog = this.let {
                CustomDialog.Builder(it)
                    .setDialogType(CustomDialog.DialogType.CENTER_DIALOG)
                    .setDesign(R.layout.dialog_custom_v2)
                    .setCancelable(false)
                    .setRoundedCorners(16)
                    .addButton(
                        type = "INFO",
                        text = "ACTUALIZAR",
                        stuffed = false,
                        listener = {
                            val url = AppConfig.getUpdateUrl()
                            val intent = Intent(Intent.ACTION_VIEW, url?.toUri())
                            startActivity(intent)
                        }
                    )
                    .build()
            }

            val linearLayoutMain: LinearLayout =
                customDialog.findView(R.id.linear_layout_dialog_custom_main)
            linearLayoutMain.background =
                AppCompatResources.getDrawable(this, R.drawable.bg_dialog_alert)

            val title: TextView = customDialog.findView(R.id.text_view_dialog_custom_title)
            title.visibility = View.VISIBLE
            title.text = "Actualización disponible"

            val animationView: LottieAnimationView =
                customDialog.findView(R.id.animation_view_custom_dialog)
            animationView.visibility = View.VISIBLE
            animationView.setAnimation(R.raw.ic_rocket)

            val description: TextView =
                customDialog.findView(R.id.text_view_dialog_custom_description)
            description.visibility = View.VISIBLE
            val textBuilder: TextSpanBuilder = TextSpanBuilder(this)
            val descriptionText: SpannableStringBuilder = textBuilder
                .append("¡Hay una nueva actualización!\n\n", Typeface.BOLD, 1.2f, null)
                .append(
                    "Asegúrate de tener siempre la última versión disponible para tener la mejor experiencia posible.\n\n",
                    Typeface.NORMAL,
                    1.0f,
                    R.color.lightTextColor
                )
                .append("Version actual: ", Typeface.BOLD, 1.0f, null)
                .append(
                    AppUtils.getVersionName(this),
                    Typeface.NORMAL,
                    1.0f,
                    R.color.lightTextColor
                )
                .append("\nNueva version: ", Typeface.BOLD, 1.0f, null)
                .append(
                    AppConfig.getUpdateVersionName(),
                    Typeface.NORMAL,
                    1.0f,
                    R.color.lightTextColor
                )
                .build()
            description.text = descriptionText
            description.gravity = Gravity.CENTER

            customDialog.show()
        }
    }
}