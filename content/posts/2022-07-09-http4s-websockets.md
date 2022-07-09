---
layout: post
title: How to Serve Web Sockets with Http4s
url: "2022/07/web-sockets-with-http4s"
date: "2022-07-09T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
# summary: Concurrency in golang can get tricky, now that generics have landed a Future library can help.
# cover:
# #   image: /assets/2022-05-17-undraw_Software_engineer_re_tnjc.png
#   hidden: true
modified_time: "2022-07-09T00:00:00.000-00:00"
---

[Web Sockets](https://en.wikipedia.org/wiki/WebSocket) are a great way to stream data from a server to a client. [Http4s](https://http4s.org/) is a fantastic http server for Scala.

The trick to serving Web Sockets with Http4s is that you need to get hold of `WebSocketBuilder2` before you can make the `HttpRoutes`. You get the WebSocketBuilder by using `withHttpWebSocketApp` on the `ServerBuilder` your using.

For example you to serve websocket data on `/ws` you could have a `routes` function like this.

```scala
def routes(ws: WebSocketBuilder2[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "ws" =>
        val send: Stream[IO, WebSocketFrame] =
          Stream.awakeEvery[IO](1.second)
            .evalMap(_ => IO(WebSocketFrame.Text("ok")))
        val receive: Pipe[IO, WebSocketFrame, Unit] =
          in => in.evalMap(frameIn => IO(println("in " + frameIn.length)))

        ws.build(send, receive)
    }
```

Then in a web client, you can subscribe to the websocket like this.

```js
const socket = new WebSocket("ws://localhost:8080/ws");

socket.onmessage = function (event) {
  console.log(`[message] Data received from server: ${event.data}`);
};
```

The complete example

```scala
import com.comcast.ip4s._
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all._
import fs2.{Pipe, Stream}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import java.time.Instant
import scala.concurrent.duration._

object Main extends IOApp {

  def routes(ws: WebSocketBuilder2[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "ws" =>
        val send: Stream[IO, WebSocketFrame] =
          Stream.awakeEvery[IO](1.second)
            .evalMap(_ => IO(WebSocketFrame.Text("ok")))
        val receive: Pipe[IO, WebSocketFrame, Unit] =
          in => in.evalMap(frameIn => IO(println("in " + frameIn.length)))

        ws.build(send, receive)
    }

  val serverResource: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpWebSocketApp(ws => routes(ws).orNotFound)
      .build


  def run(args: List[String]): IO[ExitCode] = {
    Stream
      .resource(
        serverResource >> Resource.never
      )
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
```
