# TODO

## Database
- [ ] Replace `fallbackToDestructiveMigration()` with proper migrations to preserve user data across schema changes
- [ ] Add database backup/export functionality
- [ ] Add database restore/import functionality

## Features
- [ ] Make recurring RecyclerViews expandable on button press in Analysis page
- [ ] Add date range filtering for expenses/income
- [ ] Add search functionality for expenses/income
- [ ] Add expense/income editing (currently only delete)
- [ ] Add charts/graphs for spending visualization
- [ ] Add budget notifications when approaching limits
- [ ] Add dark mode support

## Categories
- [ ] Add category icons/colors
- [ ] Add category reordering
- [ ] Allow editing category names

## UI/UX
- [ ] Add confirmation dialogs for delete actions
- [ ] Add swipe-to-delete for list items
- [ ] Add pull-to-refresh
- [ ] Improve empty state designs

## Testing
- [x] Unit tests for CategoryDao
- [x] Unit tests for ExpenseDao
- [ ] Unit tests for IncomeDao
- [ ] Unit tests for BudgetLimitDao
- [ ] UI tests for main flows

## Technical Debt
- [ ] Add proper error handling throughout the app
- [ ] Add loading states for async operations
- [ ] Consider migrating to Jetpack Compose
