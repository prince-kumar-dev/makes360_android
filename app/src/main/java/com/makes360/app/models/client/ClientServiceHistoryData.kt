package com.makes360.app.models.client

data class ClientServiceHistoryData(
    val serviceDate: String,
    val serviceDetails: String,
    var isExpanded: Boolean = false, // To track expand/collapse state
)
