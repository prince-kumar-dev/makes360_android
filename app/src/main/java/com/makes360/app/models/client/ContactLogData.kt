package com.makes360.app.models.client

data class ContactLogData(
    val date: String,
    val message: String,
    var isExpanded: Boolean = true, // To track expand/collapse state
)
