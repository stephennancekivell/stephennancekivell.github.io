---
layout: post
title: Building a work queue with Scala & FS2
date: '2020-09-12T00:00:00.000-00:00'
author: Stephen Nancekivell
tags:
modified_time: '2020-09-12T00:00:00.000-00:00'
---

I've recently improved the csv parsing in my side project [Real Email](https://isitarealemail.com) and built a work queue using [FS2](https://fs2.io). In this post I will explain how to build a work queue with cancellation.

The context for my work queue is that im validating csv files from a typical web app. This happens from the browser which then uses http polling to query for progress. The more interesting feature are that I wanted a way to cancel the processing and a way to limit the number of concurrent validations happening at once in case of a sudden spike in usage.

The imperative way to achieve the background processing is to fork off and run it in a different thread and have that work thread periodically checking for for cancellation. You could have some state to keep track of the currently running tasks, just be careful with error handling or the internal state could come out of sync. Yuck. The other annoying thing is this becomes resource intensive as you trade off delays in processing for more frequent state checking.

With [scala](https://www.scala-lang.org) and fs2 we can use `streams`, `queues` and `topics` to manage the state for us and keep everything blazingly fast. If we can have a stream of the work, doing the work is just a `parMap`. The max concurrent jobs is just the concurrency parameter to `parEvalMap`. State management is all handled by the library.

In order to have a central stream of work we can create a `Queue`, where submitting a task is just putting work on the queue. The proccessing stream is just consuming the stream. This stream will always be there running waiting for work. It wont be consuming any resounces until work comes along.

The cancellation depends a bit on the actual work being done. Cats Effect and fs2 provide good tools for cancelling work. Since my work is working on a csv file as a sub stream I can use the `stream.interruptWhen` function which accepts a stream of interuption messages, we will only ever have one of those, often none.

Then cancellation requests come as another http messsage which has no reference to the work sub stream in the parEvalMap. What we need a central way to broadcast a message to the work consumers. Thats where we use a fs2 Topic. In each parEvalMap we subscribe to the cancellation messages filter to the relevant one and use it for the interruptWhen.

```scala
import fs2.concurrent.{Queue, Topic}
import cats.effect.{ContextShift, IO}

class WorkManager(
    csvFileValidator: CsvFileValidator,
    inputQueue: Queue[IO, CsvFile],
    cancelTopic: Topic[IO, Option[CsvFileId]]
)(implicit cs: ContextShift[IO]) {

  def start(file: CsvFile): IO[Boolean] =
    inputQueue.offer1(file)

  def cancel(file: CsvFileId): IO[Unit] =
    cancelTopic.publish1(Some(file))

  def startManager: IO[Unit] =
    inputQueue.dequeue
      .parEvalMap[IO, Unit](3) { file =>
        val cancelFileStream = cancelTopic
          .subscribe(3)
          .filter(_.contains(file.id))
          .map(_ => true)

        csvFileValidator
          .runValidator(file, cancelFileStream)
      }
      .compile
      .drain
}
```

