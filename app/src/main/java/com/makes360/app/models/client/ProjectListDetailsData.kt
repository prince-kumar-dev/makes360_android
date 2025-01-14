package com.makes360.app.models.client

data class ProjectListDetailsData (
    val title: String,
    val details: Map<String, String>,
    var isSelected: Boolean = false, // To track selection
    val icon: Int // Icon for the project
)