package com.makes360.app.models.client

data class ProjectListDetailsData (
    val title: String,
    val details: Map<String, String>,
    var isSelected: Boolean = false,// To track selection
    // val isExpanded: Boolean = false, // To track expansion
    // val icon: Int // Icon for the project
)