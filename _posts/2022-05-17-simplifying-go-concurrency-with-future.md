---
layout: post
title: Simplifying Go Concurrency with Futures
date: "2022-05-17T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
summary: Concurrency in golang can get tricky, now that generics have landed a Future library can help.
image: /assets/2022-05-18-docker-dash-screen.png
modified_time: "2022-05-18T00:00:00.000-00:00"
---

# Simplifying golang concurrency with Future

Go comes with good low-level concurrency primitives that can be used to build anything. Goroutines, channels and the sync package. However, care must be taken when using these or it can lead to bugs causing data corruption, race conditions and deadlocks. Preventing these bugs in large codebases is no small feat as has been studied in the code of [Uber](https://arxiv.org/pdf/2204.00764.pdf), [Docker, Kubernetes and gRPC](https://songlh.github.io/paper/go-study.pdf).

Now that generics have landed in go, it's the perfect time to see if a Future library ([go-future](https://github.com/stephennancekivell/go-future)) can help simplify our code and make it less error-prone.

Often, concurrent code comes in a few simple patterns.

- Run some functions at the same time then merge their results together
- For a list of data, run a slow transformation across all of them
- Or you might have a stream of data that you need to transform and then send to another stream.

There are others that streaming and channels handle better, but for the majority we might be able to make it easier.

## Shared memory or message passing to get a value?

It seems silly but just getting a value from a different thread isn't straightforward in go.

### Using shared memory

Some memory is allocated for a variable, then within a go routine that shared memory is written to with the populated value. Locking or a WaitGroup is then needed to protect the value. This practice can lead to corruption and should be avoided.

```go
var recipe MyRecipe
var wg sync.WaitGroup
wg.Add(1)
go func (){
  recipe = getRecipe()
  wg.Done()
}()

wg.Wait()
// now recipe is safe to use
```

### Using channels

Channels are the built-in way of message passing in go. A reference or copy of the data is passed between threads using the channel. This is considered much safer than using shared memory.

We still need to be careful with channels though, if used incorrectly it can lead to panics or blocking.

A quick recap of channel behaviour

- Writing to a full channel **blocks** until there it has space
- Reading from a channel **blocks** until there's a result
- Reading from an empty closed chan gives **nil**.
- Writing to a closed channel **panics**
- If the channel is closed while a thread is reading from it, it **blocks forever**, unless its reading with `range`

<div style="display:flex; align-items:flex-end; padding:1rem; margin-bottom:2rem; background-color:#fcb;">
    <img style="height: 10rem;width: 12rem; margin-right:2rem;" src="/assets/2022-05-17-kitten-1.png">
    <div>
        <p>Cautious Cat says</p>
        <quote>
        "I need to be careful when using this API, I could get it muddled a few ways"
        </quote>
    </div>  
</div>

We create the channel in the main thread giving it a size of 1. Then the go routine writes the value to the channel. The main thread reads from the channel once, which blocks until its ready.

```go
var recipeChan = make(chan MyRecipe, 1)
go func (){
  recipeChan <- getRecipe()
}()
recipe := <- recipeChan
```

## Introducing Future

`Future` is a data type that holds a value that might not be ready yet. It has a Get function to get the value which waits until it's ready. Get can be called multiple times and it will reuse the value.

In summary

- Runs the function in the background goroutine
- Waits for the result when we call Get
- You can call get multiple times, returning the same result.

<div style="display:flex; align-items:flex-end; padding:1rem; margin-bottom:2rem; background-color:#fcb;">
    <div>
        <p>Clumsy Cat says,</p>
        <quote>
        "That easy enough that I could use it and it wouldn't ever get mixed up, I can stop worrying about data races, and focus on what im trying to do with the data."
        </quote>
    </div>  
    <img style="height: 12rem;width: 10rem; margin-left:2rem;" src="/assets/2022-05-17-cat-2.png">
</div>

A new future is created with a function that returns a value. The main thread then calls Get on the future which blocks until its ready.

```go
recipeFuture := future.New(func() MyRecipe {
  return getRecipe()
})

recipe := recipeFuture.Get()
```

Or even just `future.New(getRecipe)`

### Common Scenarios

Now that we have the basics, lets look at how you would solve some other common scenarios with Future.

To run two things in the background and merge there results, simply create the two futures, then call Get on them, if the order of

```go
recipeFuture := future.New(getRecipe)
ingredientsFuture := future.New(getIngredients)

recipe := recipeFuture.Get()
ingredients := ingredientsFuture.Get()
```

To get the fastest result from two things use Race.

```go
recipeFuture := future.Race(
  future.New(getInternetRecipe),
  future.New(getBookRecipe),
)
```

To transform a list of things in the background use Sequence.

```go
var vegetableFutures []future.Future[string]
// …
choppedVeges := future.Sequence(vegetableFutures).Get()
```

### But what about errors?

<div style="display:flex; align-items:flex-end; padding:1rem; margin-bottom:2rem; background-color:#fcb;">
    <img style="height: 10rem;width: 12rem; margin-right:2rem;" src="/assets/2022-05-17-kitten-1.png">
    <div>
        <p>Cautious Cat says,</p>
        <quote>
        "Future only has one value and almost every function in my code returns an error"
        </quote>
    </div>  
</div>

Future is only concerned with concurrency, multi return can easily be layered on top using tuple or something like `Result`.

For example with tuple.

```go
recipeFuture := future.New(func() tuple.T2[MyRecipe, error] {
  return tuple.New2(getRecipe(),fmt.Errorf("oops"))
})

recipe, err := recipeFuture.Get().Values()
```

### But what about performance?

<div style="display:flex; align-items:flex-end; justify-content:flex-end; padding:1rem; margin-bottom:2rem; background-color:#fcb;">
    <div>
        <p>Cautious Cat says, "I heard generics were new and might be slow."</p>
    </div>  
    <img style="height: 12rem;width: 10rem; margin-left:2rem;" src="/assets/2022-05-17-cat-2.png">
</div>

Some benchmarking has been done, and no significant slowdown was observed between the traditional approaches.

Future is intended to be used when your waiting for something that's slow, like network or filesystem access, something that's much slower than the internal memory characteristics of Go. Your mileage may vary if you're in a hot loop.

## Conclusion

If you like the look of [go-future](https://github.com/stephennancekivell/go-future), head on over to [GitHub](https://github.com/stephennancekivell/go-future) and `go get` it. If you liked this article, follow me on twitter [@hi_stephen_n](https://twitter.com/hi_stephen_n) 💙 and let me know if your doing something similar in a large codebase.
