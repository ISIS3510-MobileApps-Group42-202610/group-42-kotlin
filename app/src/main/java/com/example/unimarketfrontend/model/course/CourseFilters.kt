package com.example.unimarketfrontend.model.course

fun Course.matchesFaculty(faculty: String?): Boolean {
    return faculty == null || this.faculty.equals(faculty, ignoreCase = true)
}

fun Course.matchesDepartmentCode(departmentCode: String?): Boolean {
    return departmentCode == null || this.departmentCode.equals(departmentCode, ignoreCase = true)
}

fun Course.matchesCourseSearch(query: String): Boolean {
    if (query.isBlank()) return true

    val normalized = query.trim().lowercase()

    return code.lowercase().contains(normalized) ||
        name.lowercase().contains(normalized) ||
        faculty.lowercase().contains(normalized) ||
        departmentCode.lowercase().contains(normalized)
}

fun List<Course>.faculties(): List<String> {
    return map { it.faculty }
        .distinct()
        .sorted()
}

fun List<Course>.departmentCodesForFaculty(faculty: String?): List<String> {
    return filter { it.matchesFaculty(faculty) }
        .map { it.departmentCode }
        .distinct()
        .sorted()
}

fun List<Course>.coursesFor(
    faculty: String?,
    departmentCode: String?,
    query: String
): List<Course> {
    return filter { it.matchesFaculty(faculty) }
        .filter { it.matchesDepartmentCode(departmentCode) }
        .filter { it.matchesCourseSearch(query) }
        .sortedBy { it.code }
}

