# ATM Management System (Assignment III, 50 marks)

Android app · Java + XML · SQLite · package `com.example.atm` · minSdk 24

## Demo accounts (created automatically)
| Account | PIN | Opening balance |
|---|---|---|
| 1001 | 1234 | KES 50,000 |
| 1002 | 4321 | KES 20,000 |

## Features
| Feature | Where |
|---|---|
| PIN login, 3 wrong PINs lock the account for 60 seconds | `LoginActivity`, `DatabaseHelper.login()` |
| Create a new account (name, PIN, opening deposit) | `CreateAccountActivity` |
| Main menu, logout | `MenuActivity` |
| Balance enquiry | `BalanceActivity` |
| Deposit and withdraw (limit 40,000 per withdrawal, insufficient-funds check) | `TransactionActivity` |
| Transfer to another account, with a confirmation dialog | `TransferActivity` |
| Mini statement (last 5 transactions) | `StatementActivity` |
| Change PIN | `ChangePinActivity` |
| All data stored in SQLite; PINs stored hashed (SHA-256) | `DatabaseHelper` |
| Each money movement is one all-or-nothing database transaction | `deposit`, `withdraw`, `transfer` |

## Test script (take a screenshot at each step)
1. Open the app. Log in with 1001 / 1234. Balance should be KES 50,000.
2. Login errors: wrong PIN once (shows "Attempts left: 2"); unknown account (shows "Account not found"); three wrong PINs (locked for 60 seconds).
3. Deposit 5,000. Balance becomes 55,000.
4. Withdraw 10,000. Balance becomes 45,000.
5. Withdraw 50,000. Shows "Maximum per withdrawal is KES 40,000". Then withdraw 40,000 and then 10,000 more to see "Insufficient funds".
6. Transfer 2,000 to account 1002. Confirm in the dialog. Log in as 1002 and check the balance went up.
7. Mini statement shows the latest transactions, newest first.
8. Change PIN (1234 to any new PIN), log out, log in with the new PIN.
9. Create a new account and log in with the account number shown.

## Build
`gradle assembleDebug` (JDK 17, Gradle 8.7, SDK platform 34), then download `app/build/outputs/apk/debug/app-debug.apk`.
If the SDK is not found: `echo "sdk.dir=$HOME/android-sdk" > local.properties`.

To reset the data while testing, uninstall the app or clear its storage.
