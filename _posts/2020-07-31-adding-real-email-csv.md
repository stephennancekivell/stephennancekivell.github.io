---
layout: post
title: Adding CSV to Real Email Validations and feature validation
date: '2020-07-31T00:00:00.000-00:00'
author: Stephen Nancekivell
tags:
modified_time: '2020-07-31T00:00:00.000-00:00'
---

Im pleased to announce that on [Real Email](https://isitarealemail.com) you can now validate a csv file using some interactive features.
Real Email is a developer focused email validation service, allowing you to check if an email address is real, before sending anything and possibly hurting your email reputation through bouned emails.

![login form](/assets/2020-07-31-csv-parsing.png)


Its always been possible to possible to validate a csv file with a clever shell script but its not very user friendly, I didnt think it was a very important use case for my market. I wanted to support it but not invent much time into it. 

I started to notice a lot of the usage followed the pattern of a bulk load. Often usage would start testing lots of addresses quickly for a few hours then stop. My documentation about the the shell style parsing was one of my pages with the largest viewing time. The access logs also showed the usage had the user agent 'curl' which is the program used in the shell scripts.

Armed with this information I knew it was time to improve the UX and build an in-page csv validator. Where the user could upload any csv file to parse.

I was pleased to see the very next day it was being used.

So if your not sure what features to invest it, look for the signs. And if you need to validate any big lists of email addresses, head on over to [Real Email](https://isitarealemail.com) and get cracking.
