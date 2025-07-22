package com.pixesoj.freefirelike.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.config.GlobalConfig
import com.pixesoj.freefirelike.manager.AccountManager
import com.pixesoj.freefirelike.manager.ApiManager
import com.pixesoj.freefirelike.manager.ApiManager.ApiCallback
import com.pixesoj.freefirelike.manager.TokenManager
import com.pixesoj.freefirelike.model.Account
import com.pixesoj.freefirelike.ui.adapters.AccountAdapter
import com.pixesoj.freefirelike.utils.HelperUtils
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class InfoFragment : Fragment() {
    private var tokenManager: TokenManager? = null
    private var account: Account? = null
    private var uid: String? = null
    private var animation: LottieAnimationView? = null
    private var textResult: TextView? = null
    private var textStatus: TextView? = null
    private var input: AutoCompleteTextView? = null
    private var imageViewRecentAccounts: ImageView? = null
    private var accountInfoLayout: LinearLayout? = null
    private var linearLayoutInfoStatus: LinearLayout? = null
    private var linearLayoutAccountList: LinearLayout? = null
    private var btnLogin: Button? = null
    private var recentAccounts: MutableList<Account>? = null
    private var isSelectedAccount = false
    private var imageViewProfile: ImageView? = null
    private var lottieLoadingProfile: LottieAnimationView? = null
    private var imageViewCaptainProfile: ImageView? = null
    private var lottieLoadingCaptainProfile: LottieAnimationView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setAccount()
    }

    private fun setAccount(){
        tokenManager = activity?.let { TokenManager(it, true) }

        val linearLayoutMain = view?.findViewById<LinearLayout>(R.id.linearLayoutInfoFragmentMain)
        activity?.let {
            val statusBarHeight = HelperUtils.getStatusBarHeight(it)
            linearLayoutMain?.setPadding(0, statusBarHeight, 0, 0)
        }

        account = AccountManager.getSelectedAccount()
        recentAccounts = AccountManager.getRecentList()
        btnLogin = view?.findViewById<Button>(R.id.btnLogin)
        textStatus = view?.findViewById(R.id.textStatus)
        input = view?.findViewById(R.id.autoCompletePlayerId)
        imageViewRecentAccounts = view?.findViewById(R.id.imageViewRecentAccounts)
        animation = view?.findViewById(R.id.animationViewFragmentInfoStatus)
        textResult = view?.findViewById(R.id.textResult)
        accountInfoLayout = view?.findViewById(R.id.accountLinearLayoutInfoMain)
        linearLayoutAccountList = view?.findViewById<LinearLayout>(R.id.linearLayoutAccountList)
        linearLayoutInfoStatus = view?.findViewById<LinearLayout>(R.id.linearLayoutInfoStatus)
        imageViewProfile = view?.findViewById<ImageView>(R.id.imageViewProfile)
        lottieLoadingProfile = view?.findViewById<LottieAnimationView>(R.id.lottieLoadingProfile)
        imageViewCaptainProfile = view?.findViewById<ImageView>(R.id.imageViewCaptainProfile)
        lottieLoadingCaptainProfile = view?.findViewById<LottieAnimationView>(R.id.lottieLoadingCaptainProfile)

        btnLogin?.setOnClickListener {
            val idInput = input?.text.toString().trim()
            if (idInput.isEmpty()){
                textStatus?.let { it1 -> setStatus(it1, "El ID no puede estar vacío.") }
            } else if (idInput.length < 6){
                textStatus?.let { it1 -> setStatus(it1, "El ID es muy corto.") }
            } else {
                activity?.let {
                    val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(input?.windowToken, 0)
                }

                uid = idInput
                textStatus?.let { it1 -> setStatus(it1, "") }
                textResult?.let { it1 -> setResultStatus(it1, "Cargando informacion...") }
                animation?.let { it1 -> setAnimation(it1, R.raw.ic_loading) }
                linearLayoutInfoStatus?.visibility = View.VISIBLE
                accountInfoLayout?.visibility = View.GONE
                linearLayoutAccountList?.visibility = View.GONE
                fetchPlayerInfo()
            }
        }

        initRecentAccounts()
        if (recentAccounts?.isEmpty() == true){
            setStatus("Ingresa un ID para ver su información.", R.raw.ic_empty_ghost)
        } else {
            if (!isSelectedAccount){
                val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerAccounts)
                recyclerView?.layoutManager = LinearLayoutManager(requireContext())
                recyclerView?.adapter = AccountAdapter(recentAccounts!!,
                    onClick = { account ->
                        loadRecentAccount(account)
                        linearLayoutAccountList?.visibility = View.GONE
                        linearLayoutInfoStatus?.visibility = View.VISIBLE
                    },
                    onDelete = { account ->
                        deleteRecentAccount(account, recyclerView)
                        recentAccounts = AccountManager.getRecentList()
                        if (recentAccounts?.isEmpty() == true) {
                            linearLayoutAccountList?.visibility = View.GONE
                        }
                    })

                imageViewRecentAccounts?.visibility = View.GONE
                linearLayoutAccountList?.visibility = View.VISIBLE
            }
        }
    }

    private fun loadRecentAccount(account: Account?){
        activity?.let {
            val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input?.windowToken, 0)
            input?.setText(account!!.uid)
        }

        uid = account!!.uid
        setStatus("Cargando informacion...", R.raw.ic_loading)
        fetchPlayerInfo()
        initRecentAccounts()
    }

    private fun deleteRecentAccount(account: Account?, recyclerView: RecyclerView){
        AccountManager.removeAccount(account!!)

        val index = recentAccounts!!.indexOfFirst { it.uid == account.uid }
        if (index != -1) {
            recentAccounts!!.removeAt(index)
            recyclerView.adapter?.notifyItemRemoved(index)
        }


        if (this.account?.uid == account.uid){
            this.account = null
            AccountManager.selectAccount(null)
        }

        initRecentAccounts()
    }

    private fun initRecentAccounts(){
        recentAccounts = AccountManager.getRecentList()
        if (recentAccounts?.isEmpty() == true){
            imageViewRecentAccounts?.visibility = View.GONE
        } else {
            imageViewRecentAccounts?.visibility = View.VISIBLE
            imageViewRecentAccounts?.setOnClickListener {
                showRecentAccountsDialog(recentAccounts!!)
            }
        }
    }

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged", "InflateParams")
    private fun showRecentAccountsDialog(accounts: MutableList<Account>) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme_Transparent)
        val view = layoutInflater.inflate(R.layout.recent_accounts_bottom_sheet, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerAccounts)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AccountAdapter(accounts,
            onClick = { account ->
                loadRecentAccount(account)
                dialog.dismiss()
            },
            onDelete = { account ->
                deleteRecentAccount(account, recyclerView)
                recentAccounts = AccountManager.getRecentList()
                if (recentAccounts?.isEmpty() == true) {
                    dialog.dismiss()
                }
            })

        dialog.setContentView(view)
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun fetchPlayerInfo() {
        val url = GlobalConfig.API_INFO_URL + "info?uid=$uid"
        ApiManager.get(context, url, null, 10, 3, object : ApiCallback {
            override fun onSuccess(responseBody: String, responseElement: JsonElement?) {
                println("✅ Respuesta completa: $responseBody")

                responseElement?.asJsonObject?.let { json ->
                    val basicInfo = json.getAsJsonObject("basicInfo")
                    val accountId = basicInfo.getSafeString("accountId")
                    val accountType = basicInfo.getSafeInt("accountType")
                    val badgeCnt = basicInfo.getSafeInt("badgeCnt")
                    val badgeId = basicInfo.getSafeLong("badgeId")
                    val createAt = basicInfo.getSafeString("createAt")
                    val csMaxRank = basicInfo.getSafeInt("csMaxRank")
                    val csRank = basicInfo.getSafeInt("csRank")
                    val csRankingPoints = basicInfo.getSafeInt("csRankingPoints")
                    val exp = basicInfo.getSafeInt("exp")
                    val hasElitePass = basicInfo.getSafeBoolean("hasElitePass")
                    val headPic = basicInfo.getSafeString("headPic")
                    val lastLoginAt = basicInfo.getSafeString("lastLoginAt")
                    val level = basicInfo.getSafeInt("level")
                    val liked = basicInfo.getSafeInt("liked")
                    val maxRank = basicInfo.getSafeInt("maxRank")
                    val nickname = basicInfo.getSafeString("nickname")
                    val pinId = basicInfo.getSafeString("pinId")
                    val rank = basicInfo.getSafeInt("rank")
                    val rankingPoints = basicInfo.getSafeInt("rankingPoints")
                    val region = basicInfo.getSafeString("region")
                    val releaseVersion = basicInfo.getSafeString("releaseVersion")
                    val seasonId = basicInfo.getSafeInt("seasonId")
                    val showBrRank = basicInfo.getSafeBoolean("showBrRank")
                    val showCsRank = basicInfo.getSafeBoolean("showCsRank")
                    val title = basicInfo.getSafeLong("title")
                    val weaponSkinShows = basicInfo.getSafeArray("weaponSkinShows")

                    val captain = json.getAsJsonObject("captainBasicInfo")

                    val captainId = captain.getSafeString("accountId")
                    val captainNickname = captain.getSafeString("nickname")
                    val captainLevel = captain.getSafeInt("level")
                    val captainExp = captain.getSafeInt("exp")
                    val captainRank = captain.getSafeInt("rank")
                    val captainRankingPoints = captain.getSafeInt("rankingPoints")
                    val captainMaxRank = captain.getSafeInt("maxRank")
                    val captainRegion = captain.getSafeString("region")
                    val captainAccountType = captain.getSafeInt("accountType")
                    val captainBadgeCnt = captain.getSafeInt("badgeCnt")
                    val captainBadgeId = captain.getSafeInt("badgeId")
                    val captainBannerId = captain.getSafeInt("bannerId")
                    val captainCreateAt = captain.getSafeString("createAt")
                    val captainCsRank = captain.getSafeInt("csRank")
                    val captainCsMaxRank = captain.getSafeInt("csMaxRank")
                    val captainCsRankingPoints = captain.getSafeInt("csRankingPoints")
                    val captainHasElitePass = captain.getSafeBoolean("hasElitePass")
                    val captainHeadPic = captain.getSafeInt("headPic")
                    val captainLastLoginAt = captain.getSafeString("lastLoginAt")
                    val captainPinId = captain.getSafeInt("pinId")
                    val captainSeasonId = captain.getSafeInt("seasonId")
                    val captainReleaseVersion = captain.getSafeString("releaseVersion")
                    val captainShowBrRank = captain.getSafeBoolean("showBrRank")
                    val captainShowCsRank = captain.getSafeBoolean("showCsRank")
                    val captainShowRank = captain.getSafeBoolean("showRank")
                    val captainTitle = captain.getSafeInt("title")
                    val captainLiked = captain.getSafeInt("liked")
                    val captainGameBagShow = captain.getSafeInt("gameBagShow")

                    val clan = json.getAsJsonObject("clanBasicInfo")
                    val clanId = clan.getSafeString("clanId")
                    val clanName = clan.getSafeString("clanName")
                    val clanLevel = clan.getSafeInt("clanLevel")
                    val clanMembers = clan.getSafeInt("memberNum")
                    val clanCaptainId = clan.getSafeString("captainId")
                    val clanCapacity = clan.getSafeInt("capacity")

                    val diamond = json.getAsJsonObject("diamondCostRes")
                    val diamondCost = diamond.getSafeInt("diamondCost")

                    val pet = json.getAsJsonObject("petInfo")
                    val petId = pet.getSafeString("id")
                    val petName = pet.getSafeString("name")
                    val petExp = pet.getSafeInt("exp")
                    val petLevel = pet.getSafeInt("level")
                    val petSkill = pet.getSafeLong("selectedSkillId")
                    val petSkin = pet.getSafeLong("skinId")
                    val petSelected = pet.getSafeBoolean("isSelected")

                    val profile = json.getAsJsonObject("profileInfo")
                    val avatarId = profile.getSafeInt("avatarId")
                    val clothesTailorEffects = profile.getSafeArray("clothesTailorEffects")
                    val endTime = profile.getSafeLong("endTime")
                    val equippedSkills = profile.getSafeArray("equipedSkills")
                    val pvePrimaryWeapon = profile.getSafeInt("pvePrimaryWeapon")

                    val social = json.getAsJsonObject("socialInfo")
                    val socialAccountId = social.getSafeString("accountId")
                    val gender = social.getSafeString("gender")
                    val language = social.getSafeString("language")
                    val modePrefer = social.getSafeString("modePrefer")
                    val rankShow = social.getSafeString("rankShow")
                    val signature = social.getSafeString("signature")

                    val credit = json.getAsJsonObject("creditScoreInfo")
                    val creditScore = credit.getSafeInt("creditScore")
                    val creditEnd = credit.getSafeString("periodicSummaryEndTime")
                    val rewardState = credit.getSafeString("rewardState")

                    if (basicInfo != null){
                        activity?.let {
                            it.runOnUiThread {
                                downloadAndSetImage(GlobalConfig.API_GEN_URL + "generate?uid=$uid", imageViewProfile!!, lottieLoadingProfile!!)

                                initRecentAccounts()
                                animation?.visibility = View.GONE
                                textResult?.let { it1 -> setResultStatus(it1, "") }
                                accountInfoLayout?.visibility = View.VISIBLE
                                linearLayoutInfoStatus?.visibility = View.GONE

                                val textId = view?.findViewById<TextView>(R.id.textId)
                                val textName = view?.findViewById<TextView>(R.id.textName)
                                val textRegion = view?.findViewById<TextView>(R.id.textRegion)
                                val textLevel = view?.findViewById<TextView>(R.id.textLevel)
                                val textExp = view?.findViewById<TextView>(R.id.textExp)
                                val textLikes = view?.findViewById<TextView>(R.id.textLikes)
                                val textPrimeLevel = view?.findViewById<TextView>(R.id.textPrimeLevel)
                                val textBooyahPass = view?.findViewById<TextView>(R.id.textBooyahPass)
                                val textReleaseVersion = view?.findViewById<TextView>(R.id.textReleaseVersion)
                                val textSeason = view?.findViewById<TextView>(R.id.textSeason)

                                val textCsRank = view?.findViewById<TextView>(R.id.textCsRank)
                                val textCsRankingPoints = view?.findViewById<TextView>(R.id.textCsRankingPoints)
                                val textCsMaxRank = view?.findViewById<TextView>(R.id.textCsMaxRank)

                                val textBrRank = view?.findViewById<TextView>(R.id.textBrRank)
                                val textBrRankingPoints = view?.findViewById<TextView>(R.id.textBrRankingPoints)
                                val textBrMaxRank = view?.findViewById<TextView>(R.id.textBrMaxRank)

                                val textHonorPoints = view?.findViewById<TextView>(R.id.textHonorPoints)
                                val textEndTime = view?.findViewById<TextView>(R.id.textEndTime)
                                val textRewardsStatus = view?.findViewById<TextView>(R.id.textRewardsStatus)


                                val textLastLogin = view?.findViewById<TextView>(R.id.textLastLogin)
                                val textCreateAt = view?.findViewById<TextView>(R.id.textCreateAt)

                                textId?.text = accountId
                                textName?.text = nickname
                                textRegion?.text = HelperUtils.getRegionName(region)
                                textLevel?.text = level.toString()
                                textExp?.text = exp.toString()
                                textLikes?.text = liked.toString()
                                val booyahPass: String = if (hasElitePass){
                                    "Adquirido"
                                } else {
                                    "No adquirido"
                                }
                                textBooyahPass?.text = booyahPass
                                textReleaseVersion?.text = releaseVersion
                                textSeason?.text = seasonId.toString()

                                textCsRank?.text = csRank.toString()
                                textCsRankingPoints?.text = csRankingPoints.toString()
                                textCsMaxRank?.text = csMaxRank.toString()

                                textBrRank?.text = rank.toString()
                                textBrRankingPoints?.text = rankingPoints.toString()
                                textBrMaxRank?.text = maxRank.toString()

                                textHonorPoints?.text = creditScore.toString()
                                textEndTime?.text = HelperUtils.timestampToDateSafe(creditEnd) + "\n(" + HelperUtils.timestampToRelativeTimeSafe(creditEnd) + ")"
                                textRewardsStatus?.text = if (rewardState == "REWARD_STATE_UNCLAIMED") "No reclamadas" else "Reclamadas"

                                textLastLogin?.text = HelperUtils.timestampToDateSafe(lastLoginAt) + "\n(" + HelperUtils.timestampToRelativeTimeSafe(lastLoginAt) + ")"
                                textCreateAt?.text = HelperUtils.timestampToDateSafe(createAt) + "\n(" + HelperUtils.timestampToRelativeTimeSafe(createAt) + ")"

                                val textSocialGenre = view?.findViewById<TextView>(R.id.textSocialGenre)
                                val textSocialLanguage = view?.findViewById<TextView>(R.id.textSocialLanguage)
                                val textSocialActiveDays = view?.findViewById<TextView>(R.id.textSocialActiveDays)
                                val textSocialActiveTime = view?.findViewById<TextView>(R.id.textSocialActiveTime)
                                val textSocialGameMode = view?.findViewById<TextView>(R.id.textSocialGameMode)
                                val textSocialDescription = view?.findViewById<TextView>(R.id.textSocialDescription)

                                textSocialGenre?.text = HelperUtils.getGenderName(gender)
                                textSocialLanguage?.text = HelperUtils.getLanguageName(language)
                                textSocialActiveDays?.text = "..."
                                textSocialActiveTime?.text = "..."
                                textSocialGameMode?.text = HelperUtils.getGameModeName(modePrefer)
                                textSocialDescription?.text = signature


                                val accountLinearLayoutClanInfo = view?.findViewById<LinearLayout>(R.id.accountLinearLayoutClanInfo)
                                if (clan == null || clanId.isEmpty()) {
                                    accountLinearLayoutClanInfo?.visibility = View.GONE
                                } else {
                                    val textClanId = view?.findViewById<TextView>(R.id.textClanId)
                                    val textClanName = view?.findViewById<TextView>(R.id.textClanName)
                                    val textClanCaptainID = view?.findViewById<TextView>(R.id.textClanCaptainID)
                                    val textClanLevel = view?.findViewById<TextView>(R.id.textClanLevel)
                                    val textClanCapacity = view?.findViewById<TextView>(R.id.textClanCapacity)
                                    val textCaptainMembers = view?.findViewById<TextView>(R.id.textCaptainMembers)

                                    textClanId?.text = clanId
                                    textClanName?.text = clanName
                                    textClanCaptainID?.text = clanCaptainId
                                    textClanLevel?.text = clanLevel.toString()
                                    textClanCapacity?.text = clanCapacity.toString()
                                    textCaptainMembers?.text = clanMembers.toString()
                                    accountLinearLayoutClanInfo?.visibility = View.VISIBLE
                                }


                                val accountLinearLayoutCaptainInfo = view?.findViewById<LinearLayout>(R.id.accountLinearLayoutCaptainInfo)
                                if (captain == null || captainId.isEmpty()){
                                    accountLinearLayoutCaptainInfo?.visibility = View.GONE
                                } else{
                                    val textCaptainId = view?.findViewById<TextView>(R.id.textCaptainId)
                                    val textCaptainName = view?.findViewById<TextView>(R.id.textCaptainName)
                                    val textCaptainRegion = view?.findViewById<TextView>(R.id.textCaptainRegion)
                                    val textCaptainLevel = view?.findViewById<TextView>(R.id.textCaptainLevel)
                                    val textCaptainExp = view?.findViewById<TextView>(R.id.textCaptainExp)
                                    val textCaptainLikes = view?.findViewById<TextView>(R.id.textCaptainLikes)
                                    val textCaptainPrimeLevel = view?.findViewById<TextView>(R.id.textCaptainPrimeLevel)
                                    val textCaptainBooyahPass = view?.findViewById<TextView>(R.id.textCaptainBooyahPass)
                                    val textCaptainReleaseVersion = view?.findViewById<TextView>(R.id.textCaptainReleaseVersion)
                                    val textCaptainSeason = view?.findViewById<TextView>(R.id.textCaptainSeason)

                                    val textCaptainCsRank = view?.findViewById<TextView>(R.id.textCaptainCsRank)
                                    val textCaptainCsRankingPoints = view?.findViewById<TextView>(R.id.textCaptainCsRankingPoints)
                                    val textCaptainCsMaxRank = view?.findViewById<TextView>(R.id.textCaptainCsMaxRank)

                                    val textCaptainBrRank = view?.findViewById<TextView>(R.id.textCaptainBrRank)
                                    val textCaptainBrRankingPoints = view?.findViewById<TextView>(R.id.textCaptainBrRankingPoints)
                                    val textCaptainBrMaxRank = view?.findViewById<TextView>(R.id.textCaptainBrMaxRank)


                                    val textCaptainLastLogin = view?.findViewById<TextView>(R.id.textCaptainLastLogin)
                                    val textCaptainCreateAt = view?.findViewById<TextView>(R.id.textCaptainCreateAt)

                                    downloadAndSetImage(GlobalConfig.API_GEN_URL + "generate?uid=$captainId", imageViewCaptainProfile!!, lottieLoadingCaptainProfile!!)
                                    textCaptainId?.text = captainId
                                    textCaptainName?.text = captainNickname
                                    textCaptainRegion?.text = HelperUtils.getRegionName(captainRegion)
                                    textCaptainLevel?.text = captainLevel.toString()
                                    textCaptainExp?.text = captainExp.toString()
                                    textCaptainLikes?.text = captainLiked.toString()
                                    textCaptainPrimeLevel?.text = "..."
                                    val captainBooyahPass: String = if (captainHasElitePass){
                                        "Adquirido"
                                    } else {
                                        "No adquirido"
                                    }
                                    textCaptainBooyahPass?.text = captainBooyahPass
                                    textCaptainReleaseVersion?.text = captainReleaseVersion
                                    textCaptainSeason?.text = captainSeasonId.toString()

                                    textCaptainCsRank?.text = captainCsRank.toString()
                                    textCaptainCsRankingPoints?.text = captainCsRankingPoints.toString()
                                    textCaptainCsMaxRank?.text = captainCsMaxRank.toString()

                                    textCaptainBrRank?.text = captainRank.toString()
                                    textCaptainBrRankingPoints?.text = captainRankingPoints.toString()
                                    textCaptainBrMaxRank?.text = captainMaxRank.toString()

                                    textCaptainLastLogin?.text = HelperUtils.timestampToDateSafe(captainLastLoginAt) + "\n(" + HelperUtils.timestampToRelativeTimeSafe(captainLastLoginAt) + ")"
                                    textCaptainCreateAt?.text = HelperUtils.timestampToDateSafe(captainCreateAt) + "\n(" + HelperUtils.timestampToRelativeTimeSafe(captainCreateAt) + ")"
                                    accountLinearLayoutCaptainInfo?.visibility = View.VISIBLE
                                }

                                val accountLinearLayoutPetInfo = view?.findViewById<LinearLayout>(R.id.accountLinearLayoutPetInfo)
                                if (pet == null || petId.isEmpty()){
                                    accountLinearLayoutPetInfo?.visibility = View.GONE
                                } else{
                                    val textPetId = view?.findViewById<TextView>(R.id.textPetId)
                                    val textPetName = view?.findViewById<TextView>(R.id.textPetName)
                                    val textPetLevel = view?.findViewById<TextView>(R.id.textPetLevel)
                                    val textPetExp = view?.findViewById<TextView>(R.id.textPetExp)

                                    textPetId?.text = petId
                                    textPetName?.text = petName
                                    textPetLevel?.text = petLevel.toString()
                                    textPetExp?.text = petExp.toString()
                                    accountLinearLayoutPetInfo?.visibility = View.VISIBLE
                                }

                                isSelectedAccount = true
                                val newAccount = Account(
                                    uid = uid!!,
                                    username = nickname,
                                    region = region,
                                    likes = liked,
                                    avatarId = headPic,
                                )
                                AccountManager.addAccount(newAccount)
                                AccountManager.selectAccount(newAccount)
                                initRecentAccounts()
                            }
                        }
                    } else {
                        setStatus("No se encontró el ID en el servidor o se trata de un error.\nVerifica el ID e intenta de nuevo.", R.raw.ic_not_found)
                    }
                    println(json)
                } ?: println("❌ No se pudo interpretar el JSON")
            }

            override fun onError(e: Exception) {
                setStatus("No se encontró el ID en el servidor o se trata de un error.\nVerifica el ID e intenta de nuevo.", R.raw.ic_not_found)
            }

            override fun onNoInternet(context: Context) {
                setStatus("Parece que no hay conexión a Internet", R.raw.ic_not_found)
            }

            override fun onTimeout(context: Context) {
                setStatus("Tiempo de espera agotado.", R.raw.ic_not_found)
            }
        })
    }

    private fun setStatus(textStatus: TextView, message: String){
        textStatus.visibility = if (message.isEmpty()) View.GONE else View.VISIBLE
        textStatus.text = message
    }

    private fun setResultStatus(textResult: TextView, message: String){
        textResult.visibility = if (message.isEmpty()) View.GONE else View.VISIBLE
        textResult.text = message
    }

    private fun setAnimation(animation: LottieAnimationView, @RawRes resId: Int) {
        animation.visibility = View.VISIBLE
        animation.setAnimation(resId)
        animation.playAnimation()
    }

    fun JsonObject?.getSafeString(key: String): String {
        return try {
            this?.get(key)?.asString ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun JsonObject?.getSafeInt(key: String): Int {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0
    }

    fun JsonObject?.getSafeLong(key: String): Long {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asLong ?: 0L
    }

    fun JsonObject?.getSafeBoolean(key: String): Boolean {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean == true
    }

    fun JsonObject?.getSafeArray(key: String): JsonArray {
        return this?.getAsJsonArray(key) ?: JsonArray()
    }

    private fun setStatus(result: String, animation: Int, status: String? = ""){
        textStatus?.let { it1 -> setStatus(it1, status!!) }
        this.textResult?.let { it1 -> setResultStatus(it1, result) }
        this.animation?.let { it1 -> setAnimation(it1, animation) }
        this.linearLayoutInfoStatus?.visibility = View.VISIBLE
        this.accountInfoLayout?.visibility = View.GONE
    }

    fun downloadAndSetImage(
        url: String,
        imageView: ImageView,
        lottieLoadingProfile: LottieAnimationView
    ) {
        val freshUrl = url

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        (imageView.context as Activity).runOnUiThread {
            imageView.setImageDrawable(null)
            lottieLoadingProfile.visibility = View.VISIBLE
        }

        val request = Request.Builder()
            .url(freshUrl)
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                (imageView.context as Activity).runOnUiThread {
                    lottieLoadingProfile.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    (imageView.context as Activity).runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                        lottieLoadingProfile.visibility = View.GONE
                    }
                } else {
                    (imageView.context as Activity).runOnUiThread {
                        lottieLoadingProfile.visibility = View.GONE
                    }
                }
            }
        })
    }
}