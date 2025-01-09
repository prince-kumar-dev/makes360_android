package com.makes360.android.models

data class RoadmapStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val applicationStatus: Int = 0,
    val appliedDate: String = "",
    val shortListedDate: String = "",
    val interviewCallDate: String = "",
    val offerLetterLink: String = "",
    val joiningDate: String = "",
    val passedOutDate: String = "",
    val cardViewBgColor: Int,
)

