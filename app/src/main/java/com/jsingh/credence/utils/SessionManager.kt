package com.jsingh.credence.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jsingh.credence.domain.models.Transaction
import com.jsingh.credence.domain.parser.ParsedStatementResult

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("credence_proto_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_BUSINESS_TYPE = "business_type"
        private const val KEY_ACCOUNT_NO = "account_no"
        private const val KEY_CLOSING_BALANCE = "closing_balance"
        private const val KEY_TRANSACTIONS = "cached_transactions"
    }

    fun saveOnboardingSession(
        result: ParsedStatementResult,
        phone: String = "9876543210",
        businessType: String = "Retail & Kirana Store"
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_ONBOARDED, true)
            putString(KEY_USER_NAME, result.accountHolderName ?: "Verified Business")
            putString(KEY_USER_PHONE, phone)
            putString(KEY_BUSINESS_TYPE, businessType)
            putString(KEY_ACCOUNT_NO, result.maskedAccountNumber ?: "XXXX1234")
            putFloat(KEY_CLOSING_BALANCE, (result.closingBalance ?: 0.0).toFloat())
            putString(KEY_TRANSACTIONS, gson.toJson(result.transactions))
            apply()
        }
    }

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_IS_ONBOARDED, false)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "Business Owner") ?: "Business Owner"
    fun getBusinessType(): String = prefs.getString(KEY_BUSINESS_TYPE, "General Merchant") ?: "General Merchant"
    fun getAccountNumber(): String = prefs.getString(KEY_ACCOUNT_NO, "XXXX1234") ?: "XXXX1234"
    fun getClosingBalance(): Double = prefs.getFloat(KEY_CLOSING_BALANCE, 0f).toDouble()

    fun getCachedTransactions(): List<Transaction> {
        val json = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Essential for prototype testing: wipes state with one call
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}