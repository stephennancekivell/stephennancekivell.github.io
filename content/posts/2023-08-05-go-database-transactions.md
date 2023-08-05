---
layout: post
title: "Mastering Database Transactions in Go: Strategies and Best Practices"
date: "2023-08-04T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
cover:
  image: /assets/2023-08-04-go-gophers-building-db-castle.png
  hidden: true
---

![Create an Illustration of Go Gophers Building a Database Castle of Transactions!](/assets/2023-08-04-go-gophers-building-db-castle.png)

## Basics of Database Transactions in Go

Before diving into strategies, let's cover the basics of managing database transactions in Go. In Go, transactions are typically managed using the `BeginTx` function from the database/sql package. The basic flow for a transaction involves acquiring a transaction, performing operations, and then either committing or rolling back the transaction.

```go
tx, err := db.BeginTx(...)

// Perform database operations
_, err := tx.ExecContext(...)
if err != nil {
    return tx.Rollback()
}

tx.Commit()
```



# Strategy 1: Using Defer

One strategy to manage transactions is by using the `defer` statement to ensure a transaction is always either committed or rolled back. This strategy guarantees that the transaction will be properly handled even in the presence of errors.

```go
// update the users name and increment their updates count.
func UpdateUserName(ctx context.Context, userId string, name string) error {
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        return err
    }
    // Always do a rollback at the end of the function.
    // If committed this is a noop.
    // If there was an error this closes the transaction.
    defer tx.Rollback()

    rows, err := tx.QueryRowContext(ctx, `select updates from users where id = ?`, userId)
    if err != nil {
        return err
    }
    var updates int
    err := rows.Scan(&updates)
    if err != nil {
        return err
    }

    updates += 1
    
    _, err := tx.ExecContext(
        ctx,
        `update users set name = ?, updates = ? where id = ?`,
        name,
        updates,
        userId,
    )
    if err != nil {
        return err
    }
    return tx.Commit() // commit the transaction to save the work
}
```

## Strategy 2: Rollback per Error

Another strategy involves handling errors explicitly and rolling back the transaction when needed. This approach gives you more fine-grained control over transaction management.

The diff to the example code from the first strategy is.
```diff
- defer tx.Rollback()
..
-if err != nil {
-    return err
-}
+if err != nil {
+    tx.Rollback()
+    return err
+}
..
..
-if err != nil {
-    return err
-}
+if err != nil {
+    tx.Rollback()
+    return err
+}
```

## About the strategies

In these small examples the difference might seem trivial, but in a large, evolving production codebase its important to be
consistent to make it easier to avoid mistakes.

# Tips for structuring your code in a large codebase.

When dealing with a large codebase, maintaining clarity and consistency is vital. Here are some tips to structure your code effectively:

## 1. Adopt a Consistent Process for Managing Transactions

Choose one of the strategies mentioned above and stick with it consistently throughout your codebase. This ensures that all transactions are managed in a uniform manner, making it easier for developers to understand and maintain the code.

## 2. Ensure Each Transaction Has Clear Ownership

With a large enough codebase you will want eventually need to share code between a transactional function and a non-transactional
function. When doing this make sure its clear who the owner of the transaction is, and have a strategy for managing errors.

For example, in the `UpdateUserName`` example, if we wanted to share a function for loading a user, we could create a helper function that accepts either a database connection or a transaction, ensuring the transaction is handled correctly:

```go
// Find the user with the provided connection which can be either the db or a transaction.
func findUserFromConn(ctx context.Context, c *database.Conn, userId string) (User, error) {
    rows, err := c.QueryContext(ctx, ...)
    if err != nil {
        return nil, err
    }
    var user User
    err := rows.Scan(&user)
    if err != nil {
        return nil, err
    }
    return user, nil
}

func FindUser(ctx context.Context, userId string) (User, error) {
    return findUserFromConn(ctx, db, userId)
}

func UpdateUserName(ctx context.Context, userId string, name string) error {
    tx, err := db.BeginTx(ctx,nil)
    if err != nil {
        return err
    }
    defer tx.Rollback()

    user, err := findUserFromConn(ctx, tx, userId)
    if err != nil {
        return err
    }

    updates := user.updates + 1
    
    ...
}
```

By doing this, the findUserFromConn function doesn't need to know if it's inside a transaction, and the error handling will be handled appropriately.

## 3. Avoid Overusing Transactions

Remember that transactions come with an overhead, so avoid using them unnecessarily. If you don't need to write data, consider whether you truly require a transaction or if a read-only query would be sufficient.

Following these tips will help you maintain a clean and scalable codebase while effectively managing database transactions in Go.
