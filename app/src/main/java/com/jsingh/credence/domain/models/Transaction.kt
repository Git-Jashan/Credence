package com.jsingh.credence.domain.models

data class Transaction(
    val title: String,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val date: String,
    val time: String,
    val timestamp: Long,
    val merchantName: String,
    val transactionType: String,
    val account: String,
    val source: String,
    val phoneNumber: String?,
    val runningBalance: Double
)