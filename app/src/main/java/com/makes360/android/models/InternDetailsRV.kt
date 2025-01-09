package com.makes360.android.models

data class InternDetailsRV(
    val icon: Int,
    val title: String = "",
    val email: String = "",
    val stipendLink: String = "",
    val profileLink: String = "",
    val offerLetterLink: String = "",
    val resumeLink: String = "",
    val videoResumeLink: String = "",
    val certificateLink: String = "",
    val applicationStatus: Int = -1
)
