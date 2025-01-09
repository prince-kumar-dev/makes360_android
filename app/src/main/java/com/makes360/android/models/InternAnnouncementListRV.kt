package com.makes360.android.models

data class InternAnnouncementListRV(
    val date: String,
    val message: String,
    var isExpanded: Boolean = true, // To track expand/collapse state
)
