package com.makes360.app.models.client

data class ClientTraineeProfileCategory (
    val title: String,
    val details: Map<String, String>,
    var isExpanded: Boolean = true, // To track expand/collapse state
    val icon: Int, // Add an icon property
)