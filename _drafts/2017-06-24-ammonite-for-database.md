---
layout: post
title: SQL Database
date: '2017-06-24T00:00:00.000-00:00'
author: Stephen Nancekivell
tags: 
modified_time: '2017-06-24T00:00:00.000-00:00'
---

[Ammonite](http://ammonite.io) is great for those database jobs that are too complicated for SQL alone. This example uses [ScalikeJDBC]("http://scalikejdbc.org/") to update some rows.

```scala
import $ivy.{
  `org.scalikejdbc::scalikejdbc:3.0.0`,
  `ch.qos.logback:logback-classic:1.2.3`,
  `mysql:mysql-connector-java:5.1.6`
}

Class.forName("com.mysql.jdbc.Driver")
import scalikejdbc._, scalikejdbc._
ConnectionPool.singleton("jdbc:mysql://localhost/database", "root", "")
implicit val session = AutoSession

val users =
  sql"""select id, email from users"""
  .map(rs => rs.long("id") -> rs.string("email")).list.apply() 
users: List[(Long, String)] = List((1L, "foo@bar"), (2L, "bar@baz"))

def isNormalised(email: String): Boolean = ???
def normaliseEmail(email: String): String = ???

val usersWithInvalidEmail =
  users.filterNot { case (id, email) => isNormalised(email) }
usersWithInvalidEmail: List[(Long, String)] = List((1L, "foo@bar"), (2L, "bar@baz"))

usersWithInvalidEmail.foreach { case (id, email) =>
  val updatedEmail = normaliseEmail(email)
  sql"""update users set email = ${updatedEmail} where id = ${id};""".update.apply() 
}
```