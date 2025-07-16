package com.pixesoj.freefirelike.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.ui.SettingsActivity
import com.pixesoj.freefirelike.utils.HelperUtils
import java.net.URLEncoder
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pixesoj.freefirelike.manager.AccountManager
import com.pixesoj.freefirelike.ui.adapters.AccountAdapter
import org.w3c.dom.Text

class MenuFragment : Fragment() {

    private var relativeLayoutMenuSettingsItem: RelativeLayout? = null
    private var relativeLayoutClickableMenuContactItem: RelativeLayout? = null
    private var relativeLayoutClickableMenuAutoLikeItem: RelativeLayout? = null
    private var textViewFragmentMenuVersion: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val linearLayoutMain = view.findViewById<LinearLayout>(R.id.linearLayoutInfoFragmentMain)

        activity?.let {
            val statusBarHeight = HelperUtils.getStatusBarHeight(it)
            linearLayoutMain?.setPadding(0, statusBarHeight, 0, 0)
        }

        init()
    }

    private fun init(){
        relativeLayoutMenuSettingsItem = view?.findViewById<RelativeLayout>(R.id.relativeLayoutMenuSettingsItem)
        relativeLayoutClickableMenuContactItem = view?.findViewById<RelativeLayout>(R.id.relativeLayoutClickableMenuContactItem)
        relativeLayoutClickableMenuAutoLikeItem = view?.findViewById<RelativeLayout>(R.id.relativeLayoutClickableMenuAutoLikeItem)
        textViewFragmentMenuVersion = view?.findViewById<TextView>(R.id.textViewFragmentMenuVersion)

        relativeLayoutMenuSettingsItem?.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }

        relativeLayoutClickableMenuContactItem?.setOnClickListener {
            val telegramUsername = "pixesoj"
            val message = "Hola, necesito ayuda con la app FireLike"
            val url = "https://t.me/$telegramUsername?text=${URLEncoder.encode(message, "UTF-8")}"

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        relativeLayoutClickableMenuAutoLikeItem?.setOnClickListener {
            var recentAccounts = AccountManager.getAutoLikeList()
            val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme_Transparent)
            val view = layoutInflater.inflate(R.layout.recent_accounts_bottom_sheet, null)
            val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerAccounts)
            val textTitle = view.findViewById<TextView>(R.id.textTitle)
            textTitle.text = "Cuentas con AutoLike"

            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = AccountAdapter(recentAccounts,
                onClick = { account ->

                },
                onDelete = { account ->
                    AccountManager.removeAccount(account)

                    val index = recentAccounts.indexOfFirst { it.uid == account.uid }
                    if (index != -1) {
                        recentAccounts.removeAt(index)
                        recyclerView.adapter?.notifyItemRemoved(index)
                    }

                    recentAccounts = AccountManager.getRecentList()
                    if (recentAccounts.isEmpty() == true) {
                        dialog.dismiss()
                    }
                })

            dialog.setContentView(view)
            dialog.show()
        }

        val versionName = requireContext()
            .packageManager
            .getPackageInfo(requireContext().packageName, 0)
            .versionName

        textViewFragmentMenuVersion?.text = versionName
    }
}