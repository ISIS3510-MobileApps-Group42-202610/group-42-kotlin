package com.example.unimarketfrontend.model.course

data class CourseDto(
    val id: Int,
    val code: String,
    val name: String,
    val faculty: String
)

data class Course(
    val id: Int,
    val code: String,
    val name: String,
    val faculty: String,
    val departmentCode: String
)

