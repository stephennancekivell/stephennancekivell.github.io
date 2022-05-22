---
layout: post
title: Building Docker Dash - Technical Decisions
url: "2021/11/launching-docker-dash"
date: "2021-11-18T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
summary: Docker Dash is my new SaaS side project, it helps teams manage the security of their docker containers. This post is about some of the technical decisions I've made while building it.
image: /assets/2021-11-18-docker-dash-screen.png
modified_time: "2021-11-18T00:00:00.000-00:00"
---

[Docker Dash](https://dockerdash.com) is my new SaaS side project, it helps teams manage the security of their docker containers.

This post is about some of the technical decisions I've made while building it.

![Docker Dash](/assets/2021-11-18-docker-dash-screen.png)

## Project Setup

With software we can always change it later on, but with a bit of planning and making the right choices up front we can set ourselves up with a good structure that will last.

### The Tech Stack

- Cloudfront
- Terraform
- Angular
- 11ty
- Scala, http4s & doobie
- PostgreSQL

### Url Structure with AWS cloud front

All web traffic goes through [AWS cloudfront](https://aws.amazon.com/cloudfront/) as a load balancer and CDN, it provides a ssl and a central place to configure any request transformations.

- `/` goes to the 11ty homepage and blog optimized for SEO
- `/home` gets forwarded to the angular SPA where logged in users see their dashboard.
- `/api` get reverse proxied to the api

I find AWS cloudfront harder to configure than nginx but being a completely managed service gives a lot more. Alternatively if I had the traffic going through a public facing nginx on a server somewhere any maintenance or migrations would need to be carefully managed.

### Postgres Database

I was tempted to use [Dynamo](https://aws.amazon.com/dynamodb) by the promise of cheap hosting, but I think it would take much more time to get going. You have to be careful with your table structure and planning.

In [PostgreSQL](https://www.postgresql.org/) and SQL you can easily alter tables and migrate data with a few statements. SQL makes everything easy and has clear upgrade paths. Im starting small with a tiny postgres docker container in the same AWS Fargate tasks. When needed I can migrate that to a dedicated RDS instance, scale vertically through 100 instance types or even a cluster it with read replicas or sharding.

### Terraform

[Terraform](https://www.terraform.io/) provides infrastructure as code, its a simple declarative language that provides upfront validation before applying changes.

Sometimes when using Terraform it seems like you have to do things twice, you have to figure out how to do what you want in the AWS ecosystem possibly by clicking through the UI then figure out how to define it in terraform code. But it gives much more, its much easier to read the terraform code than than to navigate through the AWS console to see how something works. Where you might have 5 terraform resources defined on 1 page code code that might be 5 different config screens in AWS.

Using terraform will also give a huge benefit if I ever want to copy the project as a template and do something else, or get someone else to work on it. Its clearly documented how it works.

### 11ty static site

[11ty](https://www.11ty.dev/) is a modern static site generator from the node ecosystem. Really any static site generator would have done, what I want is a bit of templating, control over the mark up and an easy way to publish markdown articles. 11ty seems much nicer than other static site generators I've used.

I want to have a clear distinction between what pages are public for SEO and what pages are for users to interact with. It might seem funny to have two website technologies they serve different needs. static site for publishing content, the SPA for user interactions and presenting custom data.

### Angular SPA

[Angular](https://angular.io/) the big web component framework, it makes it easy to create reusable interactive elements to build pages with. I've taken my [Model Driven Forms](https://stephenn.com/2020/06/angular-model-driven-forms.html) a bit further and have a few completely model driven pages. Giving the pages an encoded consistent look and feel.

### Scala http4s doobie API

The API is basically providing authentication, data processing and serving json from postgres.

[Scala](https://www.scala-lang.org/) is a high level language which lets me do a lot with little code.

[http4s](https://http4s.org/) and doobie is like a match made in heaven, powered by the cats-effect.

[Cats Effect](https://typelevel.org/cats-effect/) is an effect management library providing an IO monad, so any parallelism, rate limiting, locking or synchronization is all done with first class data types manipulated with functions. This gives everything a great plug and play feel and makes refactoring easy.

[Doobie](https://tpolecat.github.io/doobie/) is a light weight sql library with fine grained transaction control. I want to leverage the database as much as possible and don't want to be fighting an entity framework. One killer feature is the testing, doobie is able to test the syntax of queries without executing them and provides clear error messages telling you which scala types best match the postgres types.

The API is deployed as a docker container on AWS Fargate.

## Fin

Docker Dash still has a way to go, implementing features and adding bells and whistles and this architecture will take it a long way.

If you liked this article and want more tips, follow me on twitter [@hi_stephen_n](https://twitter.com/hi_stephen_n) 💙
