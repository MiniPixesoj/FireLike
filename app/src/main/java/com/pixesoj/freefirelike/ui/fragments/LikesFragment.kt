package com.pixesoj.freefirelike.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pixesoj.freefirelike.MyApp
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.builder.TextSpanBuilder
import com.pixesoj.freefirelike.config.AppConfig
import com.pixesoj.freefirelike.config.GlobalConfig
import com.pixesoj.freefirelike.config.ServerConfigs
import com.pixesoj.freefirelike.manager.AccountManager
import com.pixesoj.freefirelike.manager.ApiManager
import com.pixesoj.freefirelike.manager.ApiManager.ApiCallback
import com.pixesoj.freefirelike.manager.CustomDialog
import com.pixesoj.freefirelike.manager.TokenManager
import com.pixesoj.freefirelike.model.Account
import com.pixesoj.freefirelike.proto.LikeCount
import com.pixesoj.freefirelike.ui.adapters.AccountAdapter
import com.pixesoj.freefirelike.utils.CryptoUtils
import com.pixesoj.freefirelike.utils.HelperUtils
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.w3c.dom.Text
import java.io.IOException
import java.net.URLEncoder

class LikesFragment : Fragment() {

    private val client = OkHttpClient()
    private var account: Account? = null
    private var textStatus: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var autoLikesAccounts: MutableList<Account>? = null
    private var uniqueAccount: Account? = null
    private var btnSendLikes: TextView? = null
    private var isAvailable: String = "FALSE"
    private var remainingTime: String? = null
    private var inputLikes: AutoCompleteTextView? = null
    private var textLikes: TextView? = null
    private var timestamp: String = ""
    private var nowTimestamp: String = getCurrentTimestap()
    private var textTodayLikes: TextView? = null
    private var textTotalLikes: TextView? = null
    private var btnCheckKey: TextView? = null
    private var inputKey: AutoCompleteTextView? = null
    private var textKeyStatus: TextView? = null
    private var linearLayoutLikesFragmentEmpty: LinearLayout? = null
    private var linearLayoutLikesFragmentInfo: LinearLayout? = null
    private var textId: TextView? = null
    private var textName: TextView? = null
    private var textRegion: TextView? = null
    private var textAutoLike: TextView? = null
    private var animationViewFragmentInfoStatus: LottieAnimationView? = null
    private var relativeLayoutAutoLike: RelativeLayout? = null
    private var linearLayoutMain: LinearLayout? = null
    private var linearLayoutKeyInfo: LinearLayout? = null
    private var textKey: TextView? = null
    private var textUses: TextView? = null
    private var textRemainingUses: TextView? = null
    private var textTodayUses: TextView? = null
    private var textRemainingTodayUses: TextView? = null
    private var textAutoLikeKey: TextView? = null
    private var textAutoLikeAmount: TextView? = null
    private var textViewChangeKey: TextView? = null
    private var linearLayoutKeyButton: LinearLayout? = null
    private var verifyKey = false
    private var isUsedKey = true
    private var linearLayoutKeyNoCanUse: LinearLayout? = null
    private var textViewGetKey: TextView? = null
    private var textViewGetKey2: TextView? = null
    private var linearLayoutLikeButton: LinearLayout? = null
    private var usedKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_likes, container, false)
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        initValues()
        setUI()
        setAccount()
        startRepeatingTask()
    }

    private fun initValues(){
        linearLayoutLikesFragmentEmpty = view?.findViewById<LinearLayout>(R.id.linearLayoutLikesFragmentEmpty)
        linearLayoutLikesFragmentInfo = view?.findViewById<LinearLayout>(R.id.linearLayoutLikesFragmentInfo)
        textTodayLikes = view?.findViewById<TextView>(R.id.textTodayLikes)
        textTotalLikes = view?.findViewById<TextView>(R.id.textTotalLikes)
        btnCheckKey = view?.findViewById<TextView>(R.id.btnCheckKey)
        inputKey = view?.findViewById<AutoCompleteTextView>(R.id.autoCompleteKey)
        textKeyStatus = view?.findViewById<TextView>(R.id.textKeyStatus)
        textLikes = view?.findViewById<TextView>(R.id.textLikes)
        textId = view?.findViewById<TextView>(R.id.textId)
        textName = view?.findViewById<TextView>(R.id.textName)
        textRegion = view?.findViewById<TextView>(R.id.textRegion)
        textAutoLike = view?.findViewById<TextView>(R.id.textAutoLike)
        animationViewFragmentInfoStatus = view?.findViewById<LottieAnimationView>(R.id.animationViewFragmentInfoStatus)
        relativeLayoutAutoLike = view?.findViewById<RelativeLayout>(R.id.relativeLayoutAutoLike)
        linearLayoutMain = view?.findViewById<LinearLayout>(R.id.linearLayoutLikesFragmentMain)
        linearLayoutKeyInfo = view?.findViewById<LinearLayout>(R.id.linearLayoutKeyInfo)
        textKey = view?.findViewById<TextView>(R.id.textKey)
        textUses = view?.findViewById<TextView>(R.id.textUses)
        textRemainingUses = view?.findViewById<TextView>(R.id.textRemainingUses)
        textTodayUses = view?.findViewById<TextView>(R.id.textTodayUses)
        textRemainingTodayUses = view?.findViewById<TextView>(R.id.textRemainingTodayUses)
        textAutoLikeKey = view?.findViewById<TextView>(R.id.textAutoLikeKey)
        textAutoLikeAmount = view?.findViewById<TextView>(R.id.textAutoLikeAmount)
        textViewChangeKey = view?.findViewById<TextView>(R.id.textViewChangeKey)
        linearLayoutKeyButton = view?.findViewById<LinearLayout>(R.id.linearLayoutKeyButton)
        linearLayoutKeyNoCanUse = view?.findViewById<LinearLayout>(R.id.linearLayoutKeyNoCanUse)
        textViewGetKey = view?.findViewById<TextView>(R.id.textViewGetKey)
        textViewGetKey2 = view?.findViewById<TextView>(R.id.textViewGetKey2)
        linearLayoutLikeButton = view?.findViewById<LinearLayout>(R.id.linearLayoutLikeButton)
    }

    private fun setUI(){
        activity?.let {
            val statusBarHeight = HelperUtils.getStatusBarHeight(it)
            linearLayoutMain?.setPadding(0, statusBarHeight, 0, 0)
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun setAccount() {
        account = AccountManager.getSelectedAccount()
        if (account == null){
            linearLayoutLikesFragmentEmpty?.visibility = View.VISIBLE
            linearLayoutLikesFragmentInfo?.visibility = View.GONE
            return
        }

        val textResult = view?.findViewById<TextView>(R.id.textResult)
        textResult?.text = "Cargando datos..."

        animationViewFragmentInfoStatus?.setAnimation(R.raw.ic_loading)
        animationViewFragmentInfoStatus?.playAnimation()

        textId?.text = account!!.uid
        textName?.text = account!!.username
        textRegion?.text = HelperUtils.getRegionName(account!!.region)
        textLikes?.text = account!!.likes.toString()
        textAutoLike?.text = if (AccountManager.isAccountInAutoLike(account!!)){
            "Habilitado"
        } else{
            "Deshabilitado"
        }

        textResult?.text = "Ingresa un ID en la sección info para hacer uso de esta herramienta."
        animationViewFragmentInfoStatus?.setAnimation(R.raw.ic_empty_ghost)
        animationViewFragmentInfoStatus?.playAnimation()
        linearLayoutLikesFragmentEmpty?.visibility = View.GONE
        linearLayoutLikesFragmentInfo?.visibility = View.VISIBLE

        inputLikes = view?.findViewById<AutoCompleteTextView>(R.id.autoCompleteAmountLikes)
        btnSendLikes = view?.findViewById<TextView>(R.id.btnSendLikes)

        textStatus = view?.findViewById(R.id.textStatus)
        inputLikes?.setText("105")
        inputLikes?.isEnabled = false


        autoLikesAccounts = AccountManager.getAutoLikeList()
        relativeLayoutAutoLike?.setOnClickListener {
            if (autoLikesAccounts!!.isEmpty()) {
                Toast.makeText(requireContext(), "No hay cuentas con AutoLike.", Toast.LENGTH_SHORT).show()
            } else {
                showAutoLikeAccountsDialog(autoLikesAccounts!!)
            }
        }

        val btnAddAutoLike = view?.findViewById<TextView>(R.id.btnAddAutoLike)
        if (AccountManager.isAccountInAutoLike(account!!)){
            btnAddAutoLike?.visibility = View.GONE
        } else {
            // TEMP ACTION
            btnAddAutoLike?.visibility = View.GONE
            btnAddAutoLike?.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                        return@setOnClickListener
                    }
                }

                AccountManager.addAutoLikeAccount(account!!)
                setAccount()
            }
        }

        updateCountLikes()
        setButtonSendLikes()

        val saveKey = AppConfig.getKey()
        saveKey?.isEmpty()?.let {
            if (!it && !verifyKey){
                inputKey?.setText(saveKey)
                checkKey(saveKey)
                verifyKey = true
            }
        }

        btnCheckKey?.setOnClickListener {
            val key = inputKey?.text.toString()
            if (!key.isEmpty() && key.length == 16){
                setStatusKey()
                checkKey(key)
            } else {
                setStatusKey("La key es incorrecta.")
            }
        }

        textViewChangeKey?.setOnClickListener {
            linearLayoutKeyInfo?.visibility = View.GONE
            linearLayoutKeyButton?.visibility = View.VISIBLE
            linearLayoutLikeButton?.visibility = View.GONE
            isUsedKey = true
            usedKey = null
        }

        textViewGetKey?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, GlobalConfig.DISCORD_URL.toUri())
            startActivity(intent)
        }

        textViewGetKey2?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, GlobalConfig.DISCORD_URL.toUri())
            startActivity(intent)
        }

        if (isUsedKey){
            linearLayoutLikeButton?.visibility = View.GONE
        } else {
            linearLayoutLikeButton?.visibility = View.VISIBLE
        }
    }

    private fun checkKey(key: String){
        val headers = mapOf(
            "key" to key,
            "X-API-Key" to GlobalConfig.API_BASE_KEY
        )
        btnCheckKey?.isEnabled = false
        setProgressKey("Verificando key...")
        linearLayoutKeyInfo?.visibility = View.GONE
        ApiManager.get(context, GlobalConfig.API_BASE_URL + "/keys/check", headers, 15, 3, object : ApiCallback {
            override fun onSuccess(
                responseBody: String?,
                responseElement: JsonElement?
            ) {
                responseElement?.asJsonObject?.let { json ->
                    activity!!.runOnUiThread {
                        btnCheckKey?.isEnabled = true
                        setProgressKey()
                        Toasty.info(
                            requireActivity(),
                            "Key verificada exitosamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (json.getSafeString("status") == "ok"){
                            usedKey = key
                            AppConfig.setKey(key)
                            textKey?.text = key
                            textUses?.text = json.getSafeString("uses")
                            textRemainingUses?.text = json.getSafeString("remaining_uses")
                            textTodayUses?.text = if (json.getSafeString("daily_uses") == "0") "No especificado" else json.getSafeString("daily_uses")
                            textRemainingTodayUses?.text = if (json.getSafeString("remaining_daily_uses") == "0") "No especificado" else json.getSafeString("remaining_daily_uses")
                            textAutoLikeKey?.text = if (json.getSafeString("autolike") == "true") "Habilitado" else "Deshabilitado"
                            textAutoLikeAmount?.text = if (json.getSafeString("autolike") == "true") json.getSafeString("autolike_limit") else "Nada"
                            isUsedKey = !json.getSafeBoolean("can_use")
                            if (isUsedKey){
                                linearLayoutKeyNoCanUse?.visibility = View.VISIBLE
                            } else {
                                linearLayoutKeyNoCanUse?.visibility = View.GONE
                            }
                            linearLayoutKeyInfo?.visibility = View.VISIBLE
                            linearLayoutKeyButton?.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onError(e: java.lang.Exception?) {
                activity!!.runOnUiThread {
                    btnCheckKey?.isEnabled = true
                    setProgressKey()
                    Toasty.error(
                        requireActivity(),
                        "Fallo al verificar la key, intenta de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        })
    }

    private fun setButtonSendLikes(){
        btnSendLikes?.setOnClickListener{
            if (isAvailable == "TRUE") {
                val amountLikes = inputLikes?.text.toString().toIntOrNull() ?: 0
                if (inputLikes?.text?.isEmpty() == true) {
                    textStatus?.let { setStatus(it, "La cantidad no puede estar vacía.") }
                } else if (amountLikes > 105) {
                    textStatus?.let { setStatus(it, "La cantidad máxima de likes es 100.") }
                } else {
                    textStatus?.let { setStatus(it, "") }
                    val tokenManager = activity?.let { it1 -> TokenManager(it1, true) }
                    setProgress("Obteniendo tokens del servidor...")

                    lifecycleScope.launch {
                        val users = ServerConfigs.SERVERS["BR"] ?: emptyList()
                        val total = users.size
                        var current = 0
                        val tokens = tokenManager?.getTokens("BR") { user, token ->
                            current += 1
                            activity?.runOnUiThread {
                                setProgress("Obteniendo tokens del servidor...", current, total)
                            }
                        }

                        if (tokens!!.isEmpty()) {
                            activity?.runOnUiThread {
                                setProgress()
                                Toasty.error(requireActivity(), "Ocurrió un error al obtener los tokens del servidor.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            context?.let {
                                val headers = mapOf(
                                    "key" to usedKey,
                                    "X-API-Key" to GlobalConfig.API_BASE_KEY
                                )
                                setProgress("Verificando key...")
                                ApiManager.get(context, GlobalConfig.API_BASE_URL + "/keys/check", headers, 15, 1, object : ApiCallback {
                                    override fun onSuccess(
                                        responseBody: String?,
                                        responseElement: JsonElement?
                                    ) {
                                        responseElement?.asJsonObject?.let { json ->
                                            activity!!.runOnUiThread {
                                                val status = json.getSafeString("status")
                                                if (status == "ok"){
                                                    val canUse = json.getSafeBoolean("can_use")
                                                    if (canUse){
                                                        setProgress("Enviando likes...")
                                                        sendLikes(account!!.uid, account!!.region, tokens, amountLikes) { sent, _ ->
                                                            setProgress("Listo, cargando informacion...")
                                                            ApiManager.get(context, GlobalConfig.API_INFO_URL + "info?uid=${account!!.uid}", null, 15, 1, object : ApiCallback {
                                                                override fun onSuccess(responseBody: String, responseElement: JsonElement?) {
                                                                    responseElement?.asJsonObject?.let { json ->
                                                                        val basicInfo = json.getAsJsonObject("basicInfo")
                                                                        val liked = basicInfo.getSafeInt("liked")
                                                                        val likesAdded = liked - account!!.likes
                                                                        if (basicInfo != null) {
                                                                            activity?.let {
                                                                                setProgress("Ya casi, enviando informacion...")
                                                                                ApiManager.get(context, GlobalConfig.API_BASE_URL + "add-likes/${account!!.uid}/$likesAdded/" + getCurrentTimestap(), null, 15, 1, object : ApiCallback {
                                                                                    override fun onSuccess(responseBody: String?, responseElement: JsonElement?) {
                                                                                        responseElement?.asJsonObject?.let { json ->
                                                                                            val status = json.get("status").asString
                                                                                            val timestamp = json.get("timestamp").asString
                                                                                            val nowTimestamp = json.get("now_timestamp").asString
                                                                                            val allLikes = json.getSafeInt("likes_total")
                                                                                            val todayLikes = json.getSafeInt("likes_today")

                                                                                            if (status == "ok" || status == "ignored") {
                                                                                                activity?.runOnUiThread {
                                                                                                    Handler(Looper.getMainLooper()).post {
                                                                                                        textLikes?.text = liked.toString()
                                                                                                        updateCountLikes()
                                                                                                        textTodayLikes?.text = todayLikes.toString()
                                                                                                        textTotalLikes?.text = allLikes.toString()
                                                                                                        account?.likes = liked
                                                                                                        AccountManager.selectAccount(account!!)
                                                                                                        setProgress()
                                                                                                        setTimeRemaining(timestamp, nowTimestamp)
                                                                                                        if (likesAdded == 0){
                                                                                                            Toasty.warning(activity!!, "Haz llegado al limite diario para ${account!!.username}", Toast.LENGTH_LONG).show()
                                                                                                        } else {
                                                                                                            isAvailable = "FALSE"
                                                                                                            Toasty.success(activity!!, "$likesAdded likes agregados con exito a ${account!!.username}", Toast.LENGTH_LONG).show()
                                                                                                            val headers = mapOf(
                                                                                                                "key" to usedKey,
                                                                                                                "X-API-Key" to GlobalConfig.API_BASE_KEY
                                                                                                            )
                                                                                                            ApiManager.get(context, GlobalConfig.API_BASE_URL + "/keys/use", headers, 15, 1, object : ApiCallback {
                                                                                                                override fun onSuccess(responseBody: String?, responseElement: JsonElement?) {
                                                                                                                    responseElement?.asJsonObject?.let { json ->
                                                                                                                        activity?.runOnUiThread {
                                                                                                                            val status = json.getSafeString("status")
                                                                                                                            if (status == "ok"){
                                                                                                                                textUses?.text = json.getSafeString("uses")
                                                                                                                                textRemainingUses?.text = json.getSafeString("remaining_uses")
                                                                                                                                textTodayUses?.text = if (json.getSafeString("daily_uses") == "0") "No especificado" else json.getSafeString("daily_uses")
                                                                                                                                textRemainingTodayUses?.text = if (json.getSafeString("remaining_daily_uses") == "0") "No especificado" else json.getSafeString("remaining_daily_uses")
                                                                                                                            }
                                                                                                                        }
                                                                                                                        Log.d("Api add use key", json.toString())
                                                                                                                    }
                                                                                                                }

                                                                                                                override fun onError(e: java.lang.Exception?) { }
                                                                                                            })
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    override fun onError(e: java.lang.Exception?) {
                                                                                        setProgress()
                                                                                        Toasty.error(activity!!, "Error al comprobar la informacion.", Toast.LENGTH_LONG).show()
                                                                                    }

                                                                                    override fun onNoInternet(context: Context) {
                                                                                        setProgress()
                                                                                        Toasty.error(activity!!, "Parece que no hay conexión a Internet", Toast.LENGTH_LONG).show()
                                                                                    }

                                                                                    override fun onTimeout(context: Context) {
                                                                                        setProgress()
                                                                                        Toasty.error(activity!!, "Tiempo de espera agotado.", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                })
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                override fun onError(e: Exception) {
                                                                    setProgress()
                                                                    Toasty.error(activity!!, "Error al obtener la informacion.", Toast.LENGTH_LONG).show()
                                                                }

                                                                override fun onNoInternet(context: Context) {
                                                                    setProgress()
                                                                    Toasty.error(activity!!, "Parece que no hay conexión a Internet", Toast.LENGTH_LONG).show()
                                                                }

                                                                override fun onTimeout(context: Context) {
                                                                    setProgress()
                                                                    Toasty.error(activity!!, "Tiempo de espera agotado.", Toast.LENGTH_LONG).show()
                                                                }
                                                            })
                                                        }
                                                    } else {
                                                        setProgress()
                                                        Toasty.error(activity!!, "La key no tiene usos.", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    setProgress()
                                                    Toasty.error(activity!!, "La key no es valida.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                    override fun onError(e: java.lang.Exception?) {
                                        setProgress()
                                        Toasty.error(activity!!, "Error al obtener la informacion de la key.", Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNoInternet(context: Context) {
                                        setProgress()
                                        Toasty.error(activity!!, "Parece que no hay conexión a Internet", Toast.LENGTH_LONG).show()
                                    }

                                    override fun onTimeout(context: Context) {
                                        setProgress()
                                        Toasty.error(activity!!, "Tiempo de espera agotado.", Toast.LENGTH_LONG).show()
                                    }
                                })
                            }
                        }
                    }
                }
            } else if (isAvailable == "FALSE") {
                Toasty.error(requireActivity(), "Ya has enviado likes a esta cuenta recientemente, vuelve en $remainingTime", Toast.LENGTH_LONG).show()
            } else {
                Toasty.warning(requireActivity(), "Cargando informacion...", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setTimeRemaining(futureFrom: String, nowStr: String) {
        if (isAvailable != "TRUE" && (futureFrom.isEmpty() || nowStr.isEmpty())){
            btnSendLikes?.text = "Cargando..."
            btnSendLikes?.background = activity?.getDrawable(R.drawable.bg_btn_clear)
            btnSendLikes?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            isAvailable = "LOADING"
            return
        } else if (futureFrom == "0" && nowStr == "0"){
            isAvailable = "TRUE"
            btnSendLikes?.text = "ENVIAR LIKES"
            btnSendLikes?.background = activity?.getDrawable(R.drawable.bg_btn_accent)
            btnSendLikes?.setTextColor(ContextCompat.getColor(requireContext(), R.color.bgColor))
            return
        }

        val future = futureFrom.toLongOrNull()?.plus(24 * 60 * 60)
        val now = nowStr.toLongOrNull()

        val value = if (future == null || now == null) {
            "Inválido"
        } else {
            val diff = future - now
            if (diff <= 0) {
                "Completado"
            } else {
                val hours = diff / 3600
                val minutes = (diff % 3600) / 60
                val seconds = diff % 60

                buildString {
                    if (hours > 0) append("${hours}h ")
                    if (minutes > 0 || hours > 0) append("${minutes}m ")
                    append("${seconds}s")
                }.trim()
            }
        }

        remainingTime = value
        if (value == "Completado" || value == "Inválido"){
            isAvailable = "TRUE"
            btnSendLikes?.text = "ENVIAR LIKES"
            btnSendLikes?.background = activity?.getDrawable(R.drawable.bg_btn_accent)
            btnSendLikes?.setTextColor(ContextCompat.getColor(requireContext(), R.color.bgColor))
        } else {
            isAvailable = "FALSE"
            btnSendLikes?.text = value
            btnSendLikes?.background = activity?.getDrawable(R.drawable.bg_btn_error)
            btnSendLikes?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
        setButtonSendLikes()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permiso concedido", Toast.LENGTH_SHORT).show()
                AccountManager.addAutoLikeAccount(account!!)
                setAccount()
            } else {
                Toast.makeText(requireContext(), "Permiso denegado, este permiso es obligatorio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged", "InflateParams")
    private fun showAutoLikeAccountsDialog(accounts: MutableList<Account>) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme_Transparent)
        val view = layoutInflater.inflate(R.layout.recent_accounts_bottom_sheet, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerAccounts)
        val textTitle = view.findViewById<TextView>(R.id.textTitle)
        textTitle.text = "Cuentas con AutoLike"

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AccountAdapter(accounts,
            onClick = { account ->

                dialog.dismiss()
            },
            onDelete = { account ->
                AccountManager.removeAutoLikeAccount(account)

                val index = accounts.indexOfFirst { it.uid == account.uid }
                if (index != -1) {
                    accounts.removeAt(index)
                    recyclerView.adapter?.notifyItemRemoved(index)
                }

                if (account.uid == this.account!!.uid) {
                    val textAutoLike = view?.findViewById<TextView>(R.id.textAutoLike)
                    textAutoLike?.text = if (AccountManager.isAccountInAutoLike(account)) {
                        "Habilitado"
                    } else {
                        "Deshabilitado"
                    }
                }

                autoLikesAccounts = AccountManager.getAutoLikeList()
                if (autoLikesAccounts?.isEmpty() == true) {
                    dialog.dismiss()
                }
            })

        dialog.setContentView(view)
        dialog.show()
    }

    private fun sendLikes(
        uid: String,
        region: String,
        tokens: List<String>,
        targetAmount: Int?,
        onResult: (Int, Int) -> Unit
    ) {
        var added = 0
        var sent = 0

        if (tokens.isEmpty()) {
            onResult(0, 0)
            return
        }

        val lock = Any()

        val protobuf = LikeCount.Request.newBuilder()
            .setUid(uid.toLong())
            .setType(1)
            .build()
            .toByteArray()

        val encryptedPayload = CryptoUtils.encryptAES(protobuf)
        val loopTokens = if (targetAmount == null) tokens else tokens.take(targetAmount)

        fun checkFinished() {
            if (sent == loopTokens.size || (targetAmount != null && added >= targetAmount)) {
                Handler(Looper.getMainLooper()).post {
                    onResult(sent, added)
                }
            }
        }

        for (token in loopTokens) {
            val request = Request.Builder()
                .url(GlobalConfig.API_FF_CLIENT_URL + "LikeProfile")
                .post(encryptedPayload.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 9; ASUS_Z01QD Build/PI)")
                .header("Connection", "Keep-Alive")
                .header("Accept-Encoding", "gzip")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Unity-Version", "2018.4.11f1")
                .header("X-GA", "v1 1")
                .header("ReleaseVersion", "OB49")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    synchronized(lock) {
                        sent++
                        checkFinished()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    synchronized(lock) {
                        sent++
                        activity?.runOnUiThread {
                            setProgress("Enviando likes...", sent, targetAmount)
                        }
                        if (response.isSuccessful) {
                            added++
                        }
                        response.close()
                        checkFinished()
                    }
                }
            })
        }
    }

    private fun setStatus(textStatus: TextView, message: String){
        textStatus.visibility = if (message.isEmpty()) View.GONE else View.VISIBLE
        textStatus.text = message
    }

    @SuppressLint("SetTextI18n")
    private fun showError(message: String){
        lateinit var customDialog: CustomDialog
        customDialog = activity?.let {
            CustomDialog.Builder(it)
                .setDialogType(CustomDialog.DialogType.CENTER_DIALOG)
                .setDesign(R.layout.dialog_custom_v2)
                .setCancelable(true)
                .setRoundedCorners(16)
                .addButton(
                    type = "ERROR",
                    text = "Cerrar",
                    stuffed = false,
                    listener = {
                        customDialog.dismiss()
                    }
                )
                .build()
        }!!

        val linearLayoutMain: LinearLayout =
            customDialog.findView(R.id.linear_layout_dialog_custom_main)
        linearLayoutMain.background =
            AppCompatResources.getDrawable(requireActivity(), R.drawable.bg_dialog_alert)

        val title: TextView = customDialog.findView(R.id.text_view_dialog_custom_title)
        title.visibility = View.VISIBLE
        title.text = "¡Error inesperado!"

        val animationView: LottieAnimationView =
            customDialog.findView(R.id.animation_view_custom_dialog)
        animationView.visibility = View.VISIBLE
        animationView.setAnimation(R.raw.ic_error)

        val description: TextView =
            customDialog.findView(R.id.text_view_dialog_custom_description)
        description.visibility = View.VISIBLE
        val textBuilder: TextSpanBuilder = TextSpanBuilder(requireActivity())
        val descriptionText: SpannableStringBuilder = textBuilder
            .append(message, Typeface.NORMAL, 1.0f, null)
            .build()
        description.text = descriptionText
        description.gravity = Gravity.CENTER

        customDialog.show()
    }

    fun JsonObject?.getSafeInt(key: String): Int {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0
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

    private fun updateCountLikes() {
        nowTimestamp= getCurrentTimestap()
        if (account != null && account != uniqueAccount) {

            timestamp= ""
            uniqueAccount = account

            var todayLikes: Int? = 0
            var allLikes: Int? = 0

            ApiManager.get(context, GlobalConfig.API_BASE_URL + "/get-info/${account!!.uid}", null, 15, 1, object : ApiCallback {
                override fun onSuccess(responseBody: String?, responseElement: JsonElement?) {

                    responseElement?.asJsonObject?.let { json ->
                        val status = json.get("status").asString

                        if (status == "ok") {
                            allLikes = json.get("likes_total").asInt
                            todayLikes = json.get("likes_today").asInt
                            timestamp = json.get("timestamp").asString
                            nowTimestamp = json.get("now_timestamp").asString

                            activity?.runOnUiThread {
                                textTodayLikes?.text = todayLikes.toString()
                                textTotalLikes?.text = allLikes.toString()
                            }
                        } else {

                            activity?.runOnUiThread {
                                textTodayLikes?.text = todayLikes.toString()
                                textTotalLikes?.text = allLikes.toString()
                                setTimeRemaining("0", "0")
                            }
                        }
                    }
                }

                override fun onError(e: java.lang.Exception?) { }
            })
        }

        setTimeRemaining(timestamp, nowTimestamp)
    }

    @SuppressLint("SetTextI18n")
    private fun setProgress(status: String? = "", progress: Int? = -1, total: Int? = 100){
        var linearLayoutProgressBar = view?.findViewById<LinearLayout>(R.id.LinearLayoutProgressBar)
        var linearLayoutInfosProgress = view?.findViewById<LinearLayout>(R.id.linearLayoutInfosProgress)
        var textViewProgressInfo = view?.findViewById<TextView>(R.id.textViewProgressInfo)
        var progressActivityLikes = view?.findViewById<ProgressBar>(R.id.progressActivityLikes)

        if (status!!.isEmpty() && progress == -1){
            linearLayoutProgressBar?.visibility = View.GONE
        } else {
            linearLayoutProgressBar?.visibility = View.VISIBLE

            if (status.isEmpty()){
                linearLayoutInfosProgress?.visibility = View.GONE
            } else {
                linearLayoutInfosProgress?.visibility = View.VISIBLE
                textViewProgressInfo?.text = status
            }

            if (progress == -1){
                progressActivityLikes?.isIndeterminate = true
            } else {
                linearLayoutInfosProgress?.visibility = View.VISIBLE
                progressActivityLikes?.isIndeterminate = false
                textViewProgressInfo?.text = "$status ($progress/$total)"
                progressActivityLikes?.progress = progress!!
                progressActivityLikes?.max = total!!
            }
        }
    }

    private fun getCurrentTimestap(): String {
        return (System.currentTimeMillis() / 1000).toString()
    }

    private fun setStatusKey(message: String? = null){
        if (message == null || message.isEmpty()){
            textKeyStatus?.visibility = View.GONE
        } else {
            textKeyStatus?.text = message
            textKeyStatus?.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setProgressKey(status: String? = "", progress: Int? = -1, total: Int? = 100){
        var linearLayoutProgressBar = view?.findViewById<LinearLayout>(R.id.LinearLayoutProgressBarKey)
        var linearLayoutInfosProgress = view?.findViewById<LinearLayout>(R.id.linearLayoutInfosProgressKey)
        var textViewProgressInfo = view?.findViewById<TextView>(R.id.textViewProgressInfoKey)
        var progressActivityLikes = view?.findViewById<ProgressBar>(R.id.progressActivityKey)

        if (status!!.isEmpty() && progress == -1){
            linearLayoutProgressBar?.visibility = View.GONE
            btnCheckKey?.background = activity?.getDrawable(R.drawable.bg_btn_accent)
            btnCheckKey?.setTextColor(ContextCompat.getColor(requireContext(), R.color.bgColor))
        } else {
            linearLayoutProgressBar?.visibility = View.VISIBLE
            btnCheckKey?.background = activity?.getDrawable(R.drawable.bg_btn_clear)
            btnCheckKey?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            if (status.isEmpty()){
                linearLayoutInfosProgress?.visibility = View.GONE
            } else {
                linearLayoutInfosProgress?.visibility = View.VISIBLE
                textViewProgressInfo?.text = status
            }

            if (progress == -1){
                progressActivityLikes?.isIndeterminate = true
            } else {
                linearLayoutInfosProgress?.visibility = View.VISIBLE
                progressActivityLikes?.isIndeterminate = false
                textViewProgressInfo?.text = "$status ($progress/$total)"
                progressActivityLikes?.progress = progress!!
                progressActivityLikes?.max = total!!
            }
        }
    }

    fun JsonObject?.getSafeString(key: String): String {
        return try {
            this?.get(key)?.asString ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun JsonObject?.getSafeBoolean(key: String): Boolean {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean == true
    }
}