# Sprint 4 - Marketplace-style Explore, Wishlist and User Purchases

## Feature
Marketplace-style category navigation with hierarchical course discovery, advanced filter overlay, wishlist actions, and authenticated user purchases.

## Explore redesign
The first Explore version placed too many filters at the top. The redesign follows a marketplace pattern: main categories on the left, progressive subcategory discovery on the right, and results in a separate panel.

## Course hierarchy
Courses are not displayed as a flat list. The app uses:
Product Category → Faculty → Department Code → Course.

## Advanced filter overlay
Filters are edited in an overlay with a left filter menu and right content panel, including category, faculty, academic area, course, condition, price, and sorting.

## Wishlist
Users can add and remove listings from wishlist using a heart icon in Explore, Listing Detail, and Home.

## Profile
The profile screen shows the authenticated user’s wishlist and purchase history, both fetched from the real backend.

## BQ support
The feature tracks course demand signals such as faculty selection, department selection, course filters, price filters, empty results, listing openings, and wishlist additions.

## Technical strategy
The app reuses existing listings cache, GET /courses integration, and Room course cache. Filters run locally in the ViewModel for better performance.

## Backend Integration
- `GET /api/v1/users/me/wishlist`: Load user wishlist.
- `POST /api/v1/users/me/wishlist/:id`: Add to wishlist.
- `DELETE /api/v1/users/me/wishlist/:id`: Remove from wishlist.
- `GET /api/v1/users/me/purchases`: Load purchase history.
- `GET /api/v1/courses`: Hierarchical course discovery.

## Analytics Events
- `explore_category_selected`
- `explore_faculty_selected`
- `explore_department_selected`
- `explore_course_selected`
- `explore_subcategory_selected`
- `explore_results_opened`
- `explore_filter_overlay_opened`
- `explore_condition_filter_applied`
- `explore_price_filter_applied`
- `explore_sort_selected`
- `explore_empty_results`
- `explore_listing_opened`
- `wishlist_added`
- `wishlist_removed`
- `profile_wishlist_viewed`
- `profile_purchases_viewed`
- `create_listing_course_selected`
- `listing_created`
