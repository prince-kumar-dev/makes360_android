package com.makes360.android.models

data class ProfileCategory(
    val title: String,
    val details: Map<String, String>,
    var isExpanded: Boolean = false, // To track expand/collapse state
    val icon: Int, // Add an icon property
    val name: String = "",  // for extension of intern period message
    val internID: String = "",  // for extension of intern period message
    val adminMobileNo: String = "", // for extension of intern period
    val applicationStatus: Int = 0, // for visibility of video testimonial
    val videoTestimonialLink: String = "" // for visibility of video testimonial
)

