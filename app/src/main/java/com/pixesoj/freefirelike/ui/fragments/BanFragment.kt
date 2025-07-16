package com.pixesoj.freefirelike.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.manager.AccountManager
import com.pixesoj.freefirelike.manager.ApiManager
import com.pixesoj.freefirelike.manager.ApiManager.ApiCallback
import com.pixesoj.freefirelike.model.Account
import com.pixesoj.freefirelike.utils.HelperUtils
import java.lang.Exception

class BanFragment : Fragment() {
    private var account: Account? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var checkID: String? = null
    private var isBaned: Boolean = false
    private var period: Int = 0

    private var animationStatus: Int? = null
    private var animationBanStatus: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ban, container, false)
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val linearLayoutMain = view.findViewById<LinearLayout>(R.id.linearLayoutBanFragmentMain)
        activity?.let {
            val statusBarHeight = HelperUtils.getStatusBarHeight(it)
            linearLayoutMain.setPadding(0, statusBarHeight, 0, 0)
        }

        setAccount()
        startRepeatingTask()
    }

    @SuppressLint("SetTextI18n")
    private fun setAccount() {
        val linearLayoutLikesFragmentEmpty = view?.findViewById<LinearLayout>(R.id.linearLayoutLikesFragmentEmpty)
        val linearLayoutLikesFragmentInfo = view?.findViewById<LinearLayout>(R.id.linearLayoutLikesFragmentInfo)
        val animationViewFragmentInfoStatus = view?.findViewById<LottieAnimationView>(R.id.animationViewFragmentInfoStatus)
        val textResult = view?.findViewById<TextView>(R.id.textResult)

        account = AccountManager.getSelectedAccount()
        if (account == null) {
            textResult?.text = "Ingresa un ID en la sección info para hacer uso de esta herramienta."

            if (animationStatus != R.raw.ic_empty_ghost) {
                animationViewFragmentInfoStatus?.setAnimation(R.raw.ic_empty_ghost)
                animationViewFragmentInfoStatus?.playAnimation()
                animationStatus = R.raw.ic_empty_ghost
            }

            linearLayoutLikesFragmentEmpty?.visibility = View.VISIBLE
            linearLayoutLikesFragmentInfo?.visibility = View.GONE
        } else {
            if (checkID != null && checkID == account!!.uid) {
                setInfo()
                return
            }

            textResult?.text = "Cargando informacion..."

            if (animationStatus != R.raw.ic_loading) {
                animationViewFragmentInfoStatus?.setAnimation(R.raw.ic_loading)
                animationViewFragmentInfoStatus?.playAnimation()
                animationStatus = R.raw.ic_loading
            }

            linearLayoutLikesFragmentEmpty?.visibility = View.VISIBLE
            linearLayoutLikesFragmentInfo?.visibility = View.GONE

            checkID = account!!.uid
            val url = "https://api-check-ban.vercel.app/check_ban/${account!!.uid}"
            ApiManager.get(context, url, null, 15, 1, object : ApiCallback {
                override fun onSuccess(responseBody: String?, responseElement: JsonElement?) {
                    responseElement?.asJsonObject?.let { json ->
                        val data = json.getAsJsonObject("data")
                        if (data != null) {
                            val isBaned = data.getSafeInt("is_banned")
                            val period = data.getSafeInt("period")

                            this@BanFragment.isBaned = isBaned != 0
                            this@BanFragment.period = period
                            setInfo()

                            linearLayoutLikesFragmentEmpty?.visibility = View.GONE
                            linearLayoutLikesFragmentInfo?.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onError(e: Exception?) {}
                override fun onTimeout(context: Context?) {}
                override fun onNoInternet(context: Context?) {}
            })
        }
    }

    private fun setInfo(){
        val textId = view?.findViewById<TextView>(R.id.textId)
        val textName = view?.findViewById<TextView>(R.id.textName)
        val textRegion = view?.findViewById<TextView>(R.id.textRegion)
        val textStatus = view?.findViewById<TextView>(R.id.textStatus)
        val textPeriod = view?.findViewById<TextView>(R.id.textPeriod)
        val animationViewBanStatus = view?.findViewById<LottieAnimationView>(R.id.animationViewBanStatus)
        val banStatus: String

        if (isBaned){
            if (animationBanStatus != R.raw.ic_ban){
                animationViewBanStatus?.setAnimation(R.raw.ic_ban)
                animationViewBanStatus?.playAnimation()
                animationBanStatus = R.raw.ic_ban
            }
            banStatus = "Cuenta baneada"
        } else {
            if (animationBanStatus != R.raw.ic_success_check){
                animationViewBanStatus?.setAnimation(R.raw.ic_success_check)
                animationViewBanStatus?.playAnimation()
                animationBanStatus = R.raw.ic_success_check
            }
            banStatus = "Cuenta limpia"
        }

        textId?.text = account!!.uid
        textName?.text = account!!.username
        textRegion?.text = HelperUtils.getRegionName(account!!.region)
        textPeriod?.text = getBanPeriodDescription(period, isBaned)
        textStatus?.text = banStatus
    }

    private fun startRepeatingTask() {
        runnable = object : Runnable {
            override fun run() {
                setAccount()
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(runnable)
    }

    fun JsonObject?.getSafeInt(key: String): Int {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0
    }

    fun getBanPeriodDescription(period: Int, isBanned: Boolean): String {
        return if (!isBanned) {
            "Nada"
        } else {
            when (period) {
                0 -> "Menos de 1 mes"
                1 -> "Más de 1 mes"
                else -> "Más de $period meses"
            }
        }
    }
}