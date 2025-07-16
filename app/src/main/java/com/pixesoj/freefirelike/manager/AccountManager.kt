package com.pixesoj.freefirelike.manager

import com.orhanobut.hawk.Hawk
import com.pixesoj.freefirelike.model.Account

object AccountManager {

    private const val KEY_RECENT_ACCOUNTS = "recent_accounts"
    private const val KEY_AUTO_LIKE_ACCOUNTS = "auto_like_accounts"
    private const val KEY_SELECTED_ACCOUNT = "selected_account"
    private const val KEY_STATUS = "status"

    fun getRecentList(): MutableList<Account> {
        return Hawk.get(KEY_RECENT_ACCOUNTS, mutableListOf())
    }

    fun addAccount(account: Account) {
        val currentList = getRecentList()
        currentList.removeAll { it.uid == account.uid }
        currentList.add(0, account)
        Hawk.put(KEY_RECENT_ACCOUNTS, currentList)
    }

    fun removeAccount(account: Account) {
        val currentList = getRecentList()
        val filteredList = currentList.filter { it.uid.trim() != account.uid.trim() }.toMutableList()
        Hawk.put(KEY_RECENT_ACCOUNTS, filteredList)
    }

    fun selectAccount(account: Account?) {
        if (account == null) {
            Hawk.delete(KEY_SELECTED_ACCOUNT)
        } else {
            Hawk.put(KEY_SELECTED_ACCOUNT, account)
        }
    }

    fun getSelectedAccount(): Account? {
        return Hawk.get(KEY_SELECTED_ACCOUNT, null)
    }

    fun getAutoLikeList(): MutableList<Account> {
        return Hawk.get(KEY_AUTO_LIKE_ACCOUNTS, mutableListOf())
    }

    fun addAutoLikeAccount(account: Account) {
        val currentList = getAutoLikeList()
        currentList.removeAll { it.uid == account.uid }
        currentList.add(0, account)
        Hawk.put(KEY_AUTO_LIKE_ACCOUNTS, currentList)
    }

    fun removeAutoLikeAccount(account: Account) {
        val currentList = getAutoLikeList()
        val filteredList = currentList.filter { it.uid.trim() != account.uid.trim() }.toMutableList()
        Hawk.put(KEY_AUTO_LIKE_ACCOUNTS, filteredList)
    }

    fun clearAutoLikeList() {
        Hawk.delete(KEY_AUTO_LIKE_ACCOUNTS)
    }

    fun isAccountInAutoLike(account: Account): Boolean {
        return getAutoLikeList().any { it.uid.trim() == account.uid.trim() }
    }
}