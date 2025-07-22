package com.pixesoj.freefirelike.ui.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.config.GlobalConfig
import com.pixesoj.freefirelike.model.Account
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation

class AccountAdapter(
    private var accounts: MutableList<Account>,
    private val onClick: (Account) -> Unit,
    private val onDelete: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.imageAvatar)
        val username: TextView = view.findViewById(R.id.textUsername)
        val uid: TextView = view.findViewById(R.id.textUid)
        val deleteBtn: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]
        holder.username.text = account.username
        holder.uid.text = "UID: ${account.uid}"

        holder.itemView.setOnClickListener { onClick(account) }
        holder.deleteBtn.setOnClickListener { onDelete(account) }
        val avatarId = account.avatarId
        Log.d("Test", avatarId)
        if (avatarId.isNotEmpty() && avatarId != "0"){
            Log.d("Test", "No esta vacio")
            Picasso.get()
                .load(GlobalConfig.API_GEN_URL + "api/openitems?id=${account.avatarId}")
                .placeholder(R.drawable.icon_ff)
                .fit()
                .centerCrop()
                .transform(RoundedCornersTransformation(16, 0))
                .into(holder.avatar)
        }
    }

    override fun getItemCount() = accounts.size
}