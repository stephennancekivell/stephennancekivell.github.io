---
layout: post
title: Gopher Wrangling. Effective error handling in Go
date: "2023-06-19T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
cover:
  image: /assets/2023-06-gopher-wrangling-go-errors.jpg
  hidden: true
---

When programming in Go, the amount of error handling is something that slaps you in the face. Most API's you deal with will expose errors. It can become overwhelming, but with a few tips and a guiding principle we can make handling errors easy, keep our code clean and give you the confidence that nothing is breaking in production.

![A cartoon of a crazy stressed programmer pulling their hair out in front of lots of screens showing a error exclamation marks](/assets/2023-06-gopher-wrangling-go-errors.jpg)

> A cartoon of a crazy stressed programmer pulling their hair out in front of lots of screens showing a error exclamation marks

### Guiding principle

The goal for our error handling strategy is that it should require minimal effort and provide an easy way to debug any errors that do occur.

We wont cover strategies like retrying because they are less common and also expose errors.

## 1. Always handle errors

Always handle errors. Sometimes it's tempting to skip one, you might not expect that error to ever happen. But that's why it's an exception! You need to handle it so that you can find out clearly if it ever does happen.

If you don't handle the error, the expected value will be something else and just lead to another error that will be harder to debug, or worse it could lead to data corruption.

In most cases to handle the error all you need to do is return it to the caller of your method, where they can log it.

For example, when refreshing some data you might load it, then save it. If you skip the error handling it could overwrite potentially useful data with corrupt data.

👎 Bad error handling

```go
func refresh() {
    bytes, _ := loadData()
    saveData(bytes)
}
```

👍 Good error handling

```go
func refresh() error {
    bytes, err := loadData()
    if err != nil {
        return err
    }
    saveData(bytes)
}
```

## 2. Log errors in one layer

You always want to log your errors, ideally to something that will notify you about the error, so you can fix it. There is no point logging the error multiple times at every layer. Make it the top layer's responsibility and don't log in any services or lower level code.

Make sure your logging framework is including stack traces so you can trace the error to its cause.

For example in a web app you would log the error in the http handler when returning the Internal Server status code.

👍 Good error handling

```go
func refresh() error {
    bytes, err := loadData()
    if err != nil {
        return err
    }
    saveData(bytes)
}

func (h *handlers) handleRefreshRequest(w http.ResponseWriter, r *http.Request) {
    err := refresh()
    if err != nil {
        log.Error("unexpected error processing request %w", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}
```

## 3. Returning async errors

When processing data concurrently using a go-func's, it can be annoying to return the error. But if you don't your app will be less maintainable. To handle async errors, return them via a channel to the calling thread.

👎 Bad error handling

```go
func refreshManyConcurrently() {
    go func(){
        refresh(1)
    }()

    go func(){
        refresh(2)
    }()
}
```

👍 Good error handling

```go
func refreshManyConcurrently() error {
    errors := make(chan error, 2)
    go func(){
        errors <- refresh(1)
    }()

    go func(){
        errors <- refresh(2)
    }()
    return multierror.Combine(<-errors, <- errors)
}
```

When calling functions that return a value and a possible error using a type like `Result[T]`, to wrap the response to pass on the channel.

```go
type Result[T any] struct {
    Value T
    Error error
}
```

## 4. Wrapping errors

Sometimes you want to add additional context to an error message. Eg to include the id of the request that caused the error. You can use `fmt.error` for this.

```go
err := saveToDb(user)
if err != nil {
    return fmt.errorf("unexpected error saving user. userId=%v error=%w", user.Id, err)
}
```

Usually this isn't necessary and its better to just return the error unwrapped.

## 5. Downgrade errors Warnings

There are types of errors that regularly occur during normal operation. The system might not be able to prevent them all the time, but they don't need to investigate every time. It is better to treat them as warnings rather than errors. These might be for things like timeouts or intermittent connection errors.

👍 Good error handling

```go
func (h *handlers) handleRefreshRequest(w http.ResponseWriter, r *http.Request) {
    err := refresh()
    if err != nil {
        if err == context.DeadlineExceeded {
            log.Warn("Timeout error processing request %w", err)
        } else {
            log.Error("unexpected error processing request %w", err)
        }

        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}
```
