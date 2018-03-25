---
layout: post
title: Avoiding Scala Thread Exhaustion in Scalacache
date: '2018-03-15T00:00:00.000-00:00'
author: Stephen Nancekivell
tags: 
modified_time: '2018-03-15T00:00:00.000-00:00'
---

In this post im going to talk about thread exhaustion of the scala global execution context and a curious case where it came up in a old version of [scalacache](https://github.com/cb372/scalacache). Its classic scala advice that you shouldnt use the default execution context, in this post we will look into detail why. We will look into how it is then avoided in scalacache while maximising code reuse.

### Background
Use of `Await.ready` starts a new thread on the default execution context, so if that happens enough you can run out of threads. Subsequent use of `Await` can then get into a deadlock. In scala 2.11 `Await` and `blocking` had a unbounded thread pool which could eventually start more threads than the operating system could handle and throw a exception `java.lang.OutOfMemoryError: unable to create new native thread`.

In 2.12 the thread pool is capped to the number of cpu processors. It can be configured with `-Dscala.concurrent.context.maxThreads`.

### Demonstration
The following [ammonite](https://github.com/lihaoyi/Ammonite) script shows the problem. I've made a simple example that shows real world usage that can experience the problem.

This code starts a single thread to continually execute the memoiszdEcho function which exercises scalacache. The cache duration isnt important. Then it floods the default execution context with up to 10 processes that just sleep, on my computer it maxes out at 8 threads. This starves the default execution context causing the memoized function to stop returning.


```scala
import $ivy.`com.github.cb372::scalacache-guava:0.9.4` 
import java.util.concurrent.Executors
import com.google.common.cache.CacheBuilder
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scalacache.ScalaCache
import scalacache.memoization._
import scalacache.guava.GuavaCache
import scala.concurrent.duration._

val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(5000).build[String, Object]
implicit val scalaCache = ScalaCache(GuavaCache(underlyingGuavaCache))

val context: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

def log(msg: String) = {
  println("" + java.time.LocalTime.now() + " - " + Thread.currentThread.getName + "\t- " + Thread.activeCount + " \t - " +msg)
}

def memoizedEcho(i: Int):Int = memoizeSync(3.seconds) {
  log("memo running" + i)
  i
}

// exersise memo on different thread
Future {
  while(true) {
    log("exerise memo")
    memoizedEcho(1)
    log("sleeping")
    Thread.sleep(1000)
  }
}(context)

Thread.sleep(5000) // watch memo run.

// exhaust global thread pool
(0 to 10).foreach { i =>
  println("adding to default n:"+i)
  Future {
    while(true) {
      log("sleeping " + i)
      Thread.sleep(1000)
    }
  }(scala.concurrent.ExecutionContext.global)
}
```


```
13:13:06.276 - pool-1-thread-1  - 5    - exerise memo
13:13:06.277 - pool-1-thread-1  - 5    - sleeping
13:13:07.277 - pool-1-thread-1  - 5    - exerise memo
13:13:07.277 - pool-1-thread-1  - 5    - memo running1
13:13:07.279 - pool-1-thread-1  - 5    - sleeping
13:13:08.285 - pool-1-thread-1  - 5    - exerise memo
13:13:08.285 - pool-1-thread-1  - 5    - sleeping
13:13:09.286 - pool-1-thread-1  - 5    - exerise memo
13:13:09.286 - pool-1-thread-1  - 5    - sleeping
13:13:10.288 - pool-1-thread-1  - 5    - exerise memo
13:13:10.289 - pool-1-thread-1  - 5    - memo running1
13:13:10.289 - pool-1-thread-1  - 5    - sleeping
13:13:11.290 - pool-1-thread-1  - 5    - exerise memo
13:13:11.290 - pool-1-thread-1  - 5    - sleeping

...

adding to default n:0
adding to default n:1
adding to default n:2
adding to default n:3
adding to default n:4
adding to default n:5
adding to default n:6
adding to default n:7
adding to default n:8
adding to default n:9
adding to default n:10
13:13:12.535 - scala-execution-context-global-736 - 11   - sleeping 6
13:13:12.535 - scala-execution-context-global-733 - 11   - sleeping 3
13:13:12.535 - scala-execution-context-global-735 - 11   - sleeping 5
13:13:12.535 - scala-execution-context-global-734 - 11   - sleeping 4
13:13:12.535 - scala-execution-context-global-737 - 11   - sleeping 7
13:13:12.535 - scala-execution-context-global-710 - 11   - sleeping 1
13:13:12.535 - scala-execution-context-global-694 - 11   - sleeping 0
13:13:12.535 - scala-execution-context-global-732 - 11   - sleeping 2
13:13:13.308 - pool-1-thread-1  - 11   - exerise memo
13:13:13.309 - pool-1-thread-1  - 11   - memo running1
13:13:13.540 - scala-execution-context-global-733 - 11   - sleeping 3
13:13:13.540 - scala-execution-context-global-694 - 11   - sleeping 0
13:13:13.540 - scala-execution-context-global-736 - 11   - sleeping 6
13:13:13.540 - scala-execution-context-global-710 - 11   - sleeping 1
13:13:13.540 - scala-execution-context-global-735 - 11   - sleeping 5
13:13:13.540 - scala-execution-context-global-734 - 11   - sleeping 4
13:13:13.540 - scala-execution-context-global-732 - 11   - sleeping 2
13:13:13.540 - scala-execution-context-global-737 - 11   - sleeping 7
13:13:14.541 - scala-execution-context-global-735 - 11   - sleeping 5
13:13:14.541 - scala-execution-context-global-734 - 11   - sleeping 4
13:13:14.541 - scala-execution-context-global-732 - 11   - sleeping 2
13:13:14.541 - scala-execution-context-global-736 - 11   - sleeping 6
13:13:14.541 - scala-execution-context-global-710 - 11   - sleeping 1
13:13:14.541 - scala-execution-context-global-694 - 11   - sleeping 0
13:13:14.541 - scala-execution-context-global-733 - 11   - sleeping 3

...

```


### This is surprising,
even though `memoiszdEcho` is being called from its own thread in a different pool stops. Its blocked because memoizedSync uses `Await.ready` and the default execution context internally. This is a little bit strange because its a sync api, the guava cache is stored in memory. Whats happening internally to scalacache is they are reusing some async code with `Future.successful` which itself doesnt fork but its then flatMapped with which does.


`scalacache/package.scala@0.9.4`
```scala
def synchronouslyCacheResult(result: Future[From]): Future[From] = {
  for {
    computedValue <- result
    _ <- putWithKey(key, computedValue, ttl) recover {
      case NonFatal(e) =>
        if (logger.isWarnEnabled) {
          logger.warn(s"Failed to write to cache. Key = $key", e)
        }
        result
    }
  } yield computedValue
}
```

Remember `flatMap` needs a execution context.
`scala.concurrent.Future`
```
def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S]
```


### Solved in 0.22.0
This script has the upgraded scalacache which avoids Future and the default execution context.

```scala
import $ivy.`com.github.cb372::scalacache-guava:0.22.0` 
import java.util.concurrent.Executors
import com.google.common.cache.CacheBuilder
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scalacache._
import scalacache.memoization._
import scalacache.modes.sync._
import scalacache.serialization.binary._
import scalacache.guava.GuavaCache
import scala.concurrent.duration._

val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(5000).build[String, Entry[Int]]
implicit val scalaCache = GuavaCache(underlyingGuavaCache)

val context: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

def log(msg: String) = {
  println("" + java.time.LocalTime.now() + " - " + Thread.currentThread.getName + "\t- " + Thread.activeCount + " \t - " +msg)
}

def memoizedEcho(i: Int):Int = memoizeSync(Some(3.seconds)) {
  log("memo running" + i)
  i
}

// exersise memo on different thread
Future {
  while(true) {
    log("exerise memo")
    memoizedEcho(1)
    log("sleeping")
    Thread.sleep(1000)
  }
}(context)

// exhaust global thread pool
(0 to 10).foreach { i =>
  println("adding to default n:"+i)
  Future {
    while(true) {
      log("sleeping " + i)
      Thread.sleep(1000)
    }
  } (scala.concurrent.ExecutionContext.global)
}
```

```
13:42:54.118 - scala-execution-context-global-953 - 11   - sleeping 5
13:42:54.118 - scala-execution-context-global-949 - 11   - sleeping 4
13:42:54.118 - scala-execution-context-global-951 - 11   - sleeping 7
13:42:54.118 - scala-execution-context-global-948 - 11   - sleeping 2
13:42:54.118 - scala-execution-context-global-946 - 11   - sleeping 0
13:42:55.055 - pool-1-thread-1  - 11   - exerise memo
13:42:55.055 - pool-1-thread-1  - 11   - memo running1
13:42:55.056 - pool-1-thread-1  - 11   - sleeping
13:42:55.120 - scala-execution-context-global-947 - 11   - sleeping 1
13:42:55.120 - scala-execution-context-global-950 - 11   - sleeping 3
13:42:55.120 - scala-execution-context-global-951 - 11   - sleeping 7
13:42:55.120 - scala-execution-context-global-953 - 11   - sleeping 5
...
```


Later versions of scalacache have `modes`, where the monad used or the type of effect is parametric, its not hardcoded to `Future`. This is implemented with a `scalacache/MonadError` where `modes.sync` delegates to `Mode.AsyncForId` in place of `Future.successful` But it can be swapped out to `Future` or `scalaz.concurrent.Task` or `cats.effect`.

`scalacache.AsyncForId@0.20.0`
```scala
object AsyncForId extends Async[Id] {

  ...

  def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
```


## Conclusion
I think this is a great example of how advanced scala and parametric types can bring elegant solutions and simplify some code. Scalacache can provide a simple consistant interface for many different caching engines efficiently.

### for more

[https://github.com/cb372/scalacache](https://github.com/cb372/scalacache)

[https://www.cakesolutions.net/teamblogs/demystifying-the-blocking-construct-in-scala-futures](https://www.cakesolutions.net/teamblogs/demystifying-the-blocking-construct-in-scala-futures)

[https://typelevel.org/cats-effect/](https://typelevel.org/cats-effect/)

