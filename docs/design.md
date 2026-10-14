# Family Expense Tracker: design

Stack: React frontend, Spring Boot REST backend with Spring Security, MySQL 8. Runs locally.
Schema: [V1__init.sql](../backend/src/main/resources/db/migration/V1__init.sql), applied automatically by Flyway on startup.

## 1. Data ownership and privacy rules

Each family member has a fully private account. There is no shared view and no admin role.

1. The logged-in user's id comes **only** from the JWT. No endpoint accepts a `userId` in the URL, query or body.
2. Every repository query is filtered by `user_id` (`findByIdAndUserId`, never `findById`).
3. A record owned by someone else returns **404**, not 403, so other users' ids can't be probed.
4. The database backs this up: transactions, budgets and recurring rules reference categories through `(category_id, user_id)`, so MySQL rejects any row that points at another user's category (tested).
5. Passwords are stored as BCrypt hashes. The JWT secret and DB password come from environment variables, never from code.

## 2. Tables

| Table | Purpose | Key points |
|---|---|---|
| `users` | Login and profile | email unique (stored lowercase), BCrypt hash, currency INR |
| `categories` | Income and expense categories per user | unique name per user and type; archived instead of deleted when in use |
| `transactions` | Manual income and expense entries | amount > 0, DECIMAL(12,2); `recurring_id` set when posted by a rule |
| `budgets` | Monthly limit, overall or per category | `period_month` = 1st of month; `category_id` NULL = overall |
| `recurring_expenses` | Weekly, monthly or yearly rules (expense or income, following the category) | `next_run_date` tracks the next occurrence to post |

New users get default categories on registration. Expense: Groceries, Rent, Utilities, Transport, Health, Education, Entertainment, Shopping, Other. Income: Salary, Business, Gifts, Other.

**Recurring expenses:** a scheduled job runs daily at 00:05 and once at app startup. It posts a transaction for every active rule whose `next_run_date` is today or earlier, then advances the date. Dates are always counted from the start date, so a rule starting on the 31st posts on 30 Sep and then returns to 31 Oct. The unique `(recurring_id, txn_date)` constraint stops duplicates if the job runs twice.
- Creating a rule with a past start date posts the missed occurrences right away.
- Pausing stops posting; resuming continues from today without back-filling the paused period.
- Editing a rule applies the new schedule from today; transactions already posted stay unchanged.
- Deleting a rule keeps the transactions it posted.

## 3. REST API

All endpoints except register and login require `Authorization: Bearer <jwt>`. Amounts are decimals and dates are `YYYY-MM-DD`.

**Auth**
- `POST /api/auth/register`: `{email, password, displayName}` returns 201
- `POST /api/auth/login`: `{email, password}` returns `{token, user}`
- `GET /api/auth/me`: returns the current user

**Categories**
- `GET /api/categories?type=EXPENSE&includeArchived=false`
- `POST /api/categories`, `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`: deletes it, or archives it if transactions, budgets or recurring rules use it. Returns `{deleted, archived}`. Saving an archived category with `PUT` restores it.

**Transactions**
- `GET /api/transactions?from&to&type&categoryId&q&page&size`: paged, newest first. `q` searches notes
- `POST /api/transactions`, `PUT /api/transactions/{id}`: body `{categoryId, amount, date, note}`. The type (income or expense) comes from the category
- `DELETE /api/transactions/{id}`

**Budgets**
- `GET /api/budgets?month=2026-10`: each budget with `spent` and `remaining`
- `PUT /api/budgets`: upsert `{month, categoryId|null, amount}`. Only expense categories can have budgets
- `POST /api/budgets/copy?from=2026-09&to=2026-10`: copies budgets the target month doesn't have yet
- `DELETE /api/budgets/{id}`

**Recurring expenses**
- `GET /api/recurring`, `POST /api/recurring`, `PUT /api/recurring/{id}`, `DELETE /api/recurring/{id}`: body `{categoryId, amount, description, frequency, startDate, endDate?}`
- `POST /api/recurring/{id}/pause`, `POST /api/recurring/{id}/resume`

**Reports**
- `GET /api/reports/summary?from&to`: income, expense and balance totals
- `GET /api/reports/by-category?from&to&type=EXPENSE`: totals and percentages per category
- `GET /api/reports/monthly-trend?months=12`: income and expense per month, for a bar or line chart
- `GET /api/reports/export?from&to&format=xlsx|pdf`: file download. The Excel file (Apache POI) has a Summary sheet and a Transactions sheet; the PDF (OpenPDF) has totals, a category table and every transaction. PDF amounts show "Rs." because the built-in PDF fonts lack the ₹ sign

Errors use one shape: `{status, error, message, fieldErrors?}`.

## 4. React screens

| Screen | What it shows |
|---|---|
| Login, Register | Email and password. Each family member registers separately |
| Dashboard | Month picker, income/expense/balance/budget-left cards, spending donut, 6-month bars, budget progress, recent transactions, quick add |
| Transactions | Filters (dates, type, category, note search), paged table, add/edit modal, delete |
| Budgets | Month picker, overall budget card, per-category progress, add/edit, copy last month |
| Recurring | Rules with next date and status, add/edit, pause/resume, delete, estimated monthly total |
| Categories | Expense and income lists with colors, add/edit, delete or archive, show archived |
| Reports | Date presets or a custom range, totals, category breakdowns, 12-month trend, Excel/PDF download |

The layout works on phones too: the sidebar becomes a slide-out menu.

Screenshots are in [screenshots/](screenshots/).
