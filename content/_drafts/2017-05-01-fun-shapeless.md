---
layout: post
title: Fun Shapeless
date: '2017-05-25T02:13:00.000-07:00'
author: Stephen Nancekivell
tags: 
modified_time: '2017-05-25T02:23:43.020-07:00'
---

This post is a tour of scala features discussing parametric types, type classes and type level programming.

## Programming with List[Any]

You may have come across scala code similar to bellow working with Lists of many types. This is very easy to do, and can be pattern matched against to work with the concrete types. While this works prefectly fine, it creates a maintance and testing burden so should be avoided. You never want to see the type `Any` in your code.

```scala
val xs: Seq[Any] = List(1, 2.1, "three")

xs map {
  case i: Int => ???
  case d: Double => ???
  case s: String => ???
}
```

As an example of the maintance burden, if we add a new type like..

```scala
val xs: Seq[Any] = List(1, 2.1, "three", 4l)

xs map {
  case i: Int => ???
  case d: Double => ???
  case s: String => ???
}
```
Would produce the following runtime error.

```
scala.MatchError: 4 (of class java.lang.Long)
```

Which you may not notice untill testing or you have deployed your code and a paying customer is using it. While this is trivial to fix in this small example it is much better/cheaper/faster if we can stop this at compile time.


## Type safety with sealed trait's

Type safety can be achieved by wrapping the values in a container, Encoding it in an algebriac data type (ADT). The compiler can then reason about the ADT and provide guarantees about program correctness. It can tell you if you've missed a case. Or more importantly when you add a new case, it can tell you every where you need to cater for it.

```scala
sealed trait Number
case class IntNumber(i: Int) extends Number
case class DoubleNumber(d: Double) extends Number
case class StringNumber(s: String) extends Number

val ys: Seq[Number] =
  Seq(IntNumber(1), DoubleNumber(2.1), StringNumber("three"))

ys map {
  case IntNumber(i) => ???
  case DoubleNumber(d) => ???
}
```

If you forget to add the case you get the following warning.

```scala
cmd18.sc:1: match may not be exhaustive.
It would fail on the following inputs: StringNumber(_)
```

### Bonus Warnings as Errors

If its your program is going to crash, it may as well not compile.

```scala
scalacOptions += "-Xfatal-warnings"
```

## Flexability with Type Class

A Type class is a pattern in functional programming which lets you define how some types are similar. Rather than wrapping types in something thats the same as with ADT, for Type Classes we define individual converters for how something is similar.

In this example we define the type class called `NumberHandler` which has a parametric type `[A]` and a method `handle` which returns a `String`. We define a parametric function processNumber which must take something that has a implicit instance of the type class. Then we the instances for Int and Double, which can be defined at any point later on.

```scala
trait NumberHandler[A] {
  def handle(a: A): String
}

def processNumber[A](a: A)(implicit handler: NumberHandler[A]) =
  handler.handle(a)

...

implicit val intNumberHandler = new NumberHandler[Int] {
  def handle(a: Int): String = Integer.toString(a)
}

implicit val doubleNumberHandler = new NumberHandler[Double] {
  def handle(a: Double): String = {
    if (a.isNaN)
      "Not a Number"
    else
      a.toString
  }
}
```

With the type class in place we can invoke the process number function. Note how easy it is to use, it just takes the one number type and the apropriate number handler is used implicitly.

```scala
processNumber(1)
processNumber(2.1)
```

If we make a mistake and try to run processNumber for a type that we dont have a implicit handler for a compiler error is thrown telling is.
```scala
val x: Long = 4l
processNumber(4l)

cmd6.sc:1: could not find implicit value for
  parameter handler: $sess.cmd4.NumberHandler[Long]
```

## Lists and type classes

```scala
val xs: Seq[Any] = List(1, 2.1, "three")

xs map processNumber
```

what happens ?

![what happens](/assets/whathappens.jpg)

# Type Class in List

```scala
val xs: Seq[Any] = List(1, 2.1, "three")

xs map processNumber
```

what happens ?

Compilier Error

```scala
cmd8.sc:1: could not find implicit value for
  parameter handler: $sess.cmd4.NumberHandler[A]
```

Even if we have instance.

# Compile Time vs Runtime


# Can we do better..

`List[A]`, is fixed to one type

(A,B) tuple is fixed to its length

# HList

```scala
HList[A, B <: HList]
```

```scala
sealed trait HList
sealed trait HNil extends HList
case object HNil extends HNil
case class ::[+H, +T <: HList](head: H, tail: T) extends HList
```

```scala
@ val xs = 1 :: 2.0 :: "three" :: HNil 
xs: Int :: Double :: String :: HNil = 1 :: 2.0 :: three :: HNil
@ xs.head 
res2: Int = 1
@ xs.last 
res3: String = "three"
@ xs.drop(2) 
res5: String :: HNil = three :: HNil
```

Also map and flatMap..

```scala
val xs = 1 :: 2.0 :: "three" :: HNil

xs.take(5)
```

what happens ?

![what happens](/assets/whathappens.jpg)

Compilier error

```scala
cmd26.sc:1: Implicit not found:
 shapeless.Ops.Take[
   Int :: Double :: String :: shapeless.HNil,
   shapeless.Succ[shapeless.Succ[shapeless.Succ[shapeless.Succ[shapeless.Succ[shapeless._0]]]]
   ]].
   You requested to take
   shapeless.Succ[shapeless.Succ[shapeless.Succ[shapeless.Succ[shapeless.Succ[shapeless._0]]]]]
   elements, but the HList Int :: Double :: String :: shapeless.HNil is too short.
val res26 = xs.take(5)
```

concat

```scala
val a = 1 :: 2.0 :: HNil 
a: Int :: Double :: HNil = 1 :: 2.0 :: HNil
val b = 2 :: "three" :: HNil 
b: Int :: String :: HNil = 2 :: three :: HNil
a ++ b 
res21: Int :: Double :: Int :: String :: HNil =
    1 :: 2.0 :: 2 :: three :: HNil
```

# dynamic

```scala
def giveMe = {
  if (Random.nextBoolean) 1 else "two"
}
@ val xy = giveMe :: giveMe :: HNil
```

![what happens](/assets/whathappens.jpg)


```scala
@ val xy = giveMe :: giveMe :: HNil
xy: Any :: Any :: HNil = two :: 1 :: HNil
```

Cant build HList at runtime :'(

# Encoder Example

Technique used in json parsers etc

* argonaut
* spray json
* circe
* spark database connectors

# Encoder Typeclass

```scala
trait Encoder[A] {
  def encode(a: A): String
}

implicit val intEncoder = new Encoder[Int] {
  override def encode(i: Int): String = i.toString
}

implicit val doubleEncoder = ???
implicit val stringEncoder = ???

```

# HList

```scala
implicit val nillEncoder = new Encoder[HNil] {
  override def encode(a: HNil): String = ""
}
implicit def hListEncoder[H, T <: HList](
  implicit
  encoderH: Encoder[H],
  encoderT: Encoder[T]
) = new Encoder[H :: T] {
  def encode(xs: ::[H, T]): String =
  encoderH.encode(xs.head) + "," + encoderT.encode(xs.tail)
}

hListEncoder[Int :: Double :: String :: HNil]
hListEncoder[Double :: Int :: String :: HNil]
```

Can call for any permutation for free!

But I dont use it anywhere..

# Generic

`Generic[T]`

Generic makes HList's from case classes

```scala
import shapeless.Generic

case class Foo(a: Int, b: Double, c: String)

val genFoo = Generic[Foo]
genFoo.to(Foo(1, 2.0, "three"))
res24: genFoo.Repr = 1 :: 2.0 :: three :: HNil
```


```scala
implicit def genericEncoder[A, H <: HList](
  implicit
  gen: Generic.Aux[A, H],
  hListEncoder: Encoder[H]
  ): Encoder[A] = new Encoder[A] {
    def encode(a: A): String =
      hListEncoder.encode(gen.to(a))
}

def encode[A](a: A)(implicit encoder: Encoder[A]): String =
  encoder.encode(a)
  
encode(Foo(1, 2.0, "three"))
"1,2.0,three"
```

# Thank you.

# Further reading

* Shapeless, Miles Sabin, 2011
* The Type Astronaut's Guide to Shapeless, Dave Gurnell, book 2016
* Roll your Own Shapeless, Daniel Spiewak, video presentation, scala days 2016
* Scrap your boiler plate, paper 2003