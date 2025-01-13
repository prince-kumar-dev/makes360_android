package com.makes360.app.models

data class InternAnnouncementListRV(
    val date: String,
    val message: String,
    var isExpanded: Boolean = true, // To track expand/collapse state
)
