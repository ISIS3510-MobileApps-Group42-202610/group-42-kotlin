# Sprint 4 - Course-Aware Listing Creation and Advanced Explore

## Feature
Course-aware listing creation and advanced course-based exploration.

## Difference from previous Home
Home already had offline-first listings and basic category filtering. This sprint adds a dedicated Explore flow and improves Create Listing with real course selection.

## Backend integration
The feature consumes GET /courses from the existing courses controller.

## Create Listing improvement
Sellers can select product category, product condition and a real course when creating a listing.

## Explore improvement
Buyers can filter by product category, faculty, academic area derived from course code and specific course.

## Local storage and caching
Listings reuse the existing Room offline-first cache. Courses are cached locally in Room.

## Eventual connectivity
If GET /courses fails, the app uses cached courses.

## Micro-optimization
Filters run locally in the ViewModel and do not trigger network calls on every interaction.

## Future improvement
Add explicit listing subcategories only if needed in the backend model.

