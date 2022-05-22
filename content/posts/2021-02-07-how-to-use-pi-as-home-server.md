---
layout: post
title: How to use a Raspberry Pi as a home server instead of a VPS
url: "2021/02/how-to-use-pi-as-home-server"
date: "2021-02-07T00:00:00.000-00:00"
author: Stephen Nancekivell
summary: How to use a Raspberry Pi as a home server instead of a VPS
cover:
  hidden: true
  image: /assets/2021-02-07-pi.jpg
tags:
modified_time: "2021-02-07T00:00:00.000-00:00"
---

How to use a [RaspberryPi](https://www.raspberrypi.org/) as a home server instead of A hosted VPS like [Amazon EC2](https://aws.amazon.com/ec2). A mini guide.

![Raspberry Pi](/assets/2021-02-07-pi.jpg)

- Install the Pi, enable ssh, lock down ssh to key-only login. Install web server.
- In your home router configure the Pi to have a static local ip and enable port forwarding.
- In your ISP make sure you dont have CG-NAT or any port blocking. Now you should be able to access the server over your public ip.
- In your router configure dynamic dns. Because my ISP doesnt give me a static ip. Asus provides ddns for free.

And that it. Happy hacking.

I have a few small personal projects Im moving off AWS.
For the same size server the Pi will have paid for itself in about 6 months.
