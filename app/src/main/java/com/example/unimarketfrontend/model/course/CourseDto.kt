package com.example.unimarketfrontend.model.course

import com.google.gson.annotations.SerializedName

data class CourseDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("faculty") val faculty: String?
)

data class Course(
    val id: Int,
    val code: String,
    val name: String,
    val faculty: String,
    val departmentCode: String
)

