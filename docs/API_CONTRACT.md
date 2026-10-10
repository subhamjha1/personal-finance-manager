# API Contract

> **Status: UNVERIFIED** (not executed). Derived strictly from the assignment PDF. Base path: `/api`.
> Authentication: `JSESSIONID` cookie obtained from `POST /api/auth/login`. All bodies are `application/json`.

## Error body
```json
{ "status": 400, "error": "Bad Request", "message": "Validation failed: amount: must be greater than 0",
  "details": ["amount: must be greater than 0"], "timestamp": "2026-10-10T10:00:00Z" }
```

## Auth
| Method & path | Request | Success | Errors |
|---|---|---|---|
| `POST /api/auth/register` | `{username, password, fullName, phoneNumber}` | `201 {"message":"User registered successfully","userId":1}` | 400, 409 |
| `POST /api/auth/login` | `{username, password}` | `200 {"message":"Login successful"}` + `Set-Cookie: JSESSIONID` | 400, 401 |
| `POST /api/auth/logout` | none | `200 {"message":"Logout successful"}` | 401 |

## Transactions
| Method & path | Request | Success | Errors |
|---|---|---|---|
| `POST /api/transactions` | `{amount, date, category, description?}` | `201 {id, amount, date, category, description, type}` | 400, 401 |
| `GET /api/transactions?startDate&endDate&categoryId&category&type` | – | `200 {"transactions":[…]}` newest first | 400, 401 |
| `PUT /api/transactions/{id}` | any of `{amount, category, description}` (date must be unchanged/omitted) | `200` full transaction | 400, 401, 404 |
| `DELETE /api/transactions/{id}` | – | `200 {"message":"Transaction deleted successfully"}` | 401, 404 |

## Categories
| Method & path | Request | Success | Errors |
|---|---|---|---|
| `GET /api/categories` | – | `200 {"categories":[{name,type,isCustom}]}` | 401 |
| `POST /api/categories` | `{name, type: INCOME|EXPENSE}` | `201 {name,type,isCustom:true}` | 400, 401, 409 |
| `DELETE /api/categories/{name}` | – | `200 {"message":"Category deleted successfully"}` | 400 (in use), 401, 403 (default), 404 |

## Goals
| Method & path | Request | Success | Errors |
|---|---|---|---|
| `POST /api/goals` | `{goalName, targetAmount, targetDate, startDate?}` | `201` goal | 400, 401 |
| `GET /api/goals` | – | `200 {"goals":[…]}` | 401 |
| `GET /api/goals/{id}` | – | `200` goal | 400, 401, 403, 404 |
| `PUT /api/goals/{id}` | any of `{targetAmount, targetDate, goalName}` | `200` goal | 400, 401, 403, 404 |
| `DELETE /api/goals/{id}` | – | `200 {"message":"Goal deleted successfully"}` | 401, 403, 404 |

Goal: `{id, goalName, targetAmount, targetDate, startDate, currentProgress, progressPercentage, remainingAmount}`.

## Reports
| Method & path | Success |
|---|---|
| `GET /api/reports/monthly/{year}/{month}` | `200 {month, year, totalIncome:{cat:amt}, totalExpenses:{cat:amt}, netSavings}` |
| `GET /api/reports/yearly/{year}` | `200 {year, totalIncome, totalExpenses, netSavings}` |

Both: 400 for invalid month/year, 401 unauthenticated.

## Other
`GET /actuator/health` (public, health-check), `GET /swagger-ui.html`, `GET /v3/api-docs` (public).
