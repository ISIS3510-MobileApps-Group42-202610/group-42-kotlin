package com.example.unimarketfrontend.viewmodel.explore

import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.explore.ExploreFilters
import com.example.unimarketfrontend.model.course.faculties
import com.example.unimarketfrontend.model.course.matchesCourseSearch

data class ExploreCourseDerivedData(
    val draftFilters: ExploreFilters,
    val availableFaculties: List<String>,
    val availableDepartmentCodes: List<String>,
    val availableCourses: List<Course>,
    val departmentsByFaculty: Map<String, List<String>>,
    val coursesByDepartment: Map<String, List<Course>>,
    val coursesById: Map<Int, Course>
)

object ExploreCourseDeriver {
    fun derive(
        courses: List<Course>,
        draft: ExploreFilters
    ): ExploreCourseDerivedData {
        val faculties = courses.faculties()
        val departmentsByFaculty = courses.groupBy({ it.faculty }, { it.departmentCode })
            .mapValues { it.value.distinct().sorted() }
        val coursesByDepartment = courses.groupBy { it.departmentCode }
        val coursesById = courses.associateBy { it.id }

        val selectedFaculty = draft.faculty?.takeIf { faculties.contains(it) }
        val availableDepartments = if (selectedFaculty != null) {
            departmentsByFaculty[selectedFaculty].orEmpty()
        } else {
            departmentsByFaculty.values.flatten().distinct().sorted()
        }
        val selectedDepartment = draft.departmentCode?.takeIf { availableDepartments.contains(it) }

        val availableCourses = when {
            selectedDepartment != null -> coursesByDepartment[selectedDepartment].orEmpty()
            selectedFaculty != null -> departmentsByFaculty[selectedFaculty]
                .orEmpty()
                .flatMap { coursesByDepartment[it].orEmpty() }
            draft.courseSearchQuery.isNotBlank() -> courses
            else -> emptyList()
        }.let { base ->
            if (draft.courseSearchQuery.isNotBlank()) {
                base.filter { it.matchesCourseSearch(draft.courseSearchQuery) }
            } else {
                base
            }
        }.sortedBy { it.code }

        val selectedCourse = draft.course?.takeIf { selected ->
            availableCourses.any { it.id == selected.id }
        }

        return ExploreCourseDerivedData(
            draftFilters = draft.copy(
                faculty = selectedFaculty,
                departmentCode = selectedDepartment,
                course = selectedCourse
            ),
            availableFaculties = faculties,
            availableDepartmentCodes = availableDepartments,
            availableCourses = availableCourses,
            departmentsByFaculty = departmentsByFaculty,
            coursesByDepartment = coursesByDepartment,
            coursesById = coursesById
        )
    }
}
