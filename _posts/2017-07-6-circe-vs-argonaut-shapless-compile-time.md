---
layout: post
title: Circe vs Argonaut Shapeless Compile Time
date: '2017-07-06'
author: Stephen Nancekivell
tags: 
modified_time: '2017-07-06'
---

Slow comile times in scala are a common complaint. This is usually because of advanced usage of implicit resolution and macros. The JSON libraries [ArgonautShapeless](https://github.com/alexarchambault/argonaut-shapeless) for [Argonaut](http://argonaut.io/) and [Circe](https://circe.github.io/circe/) both make heavy use of that through their use of shapeless. I love the convience this approach provides, and I think its better than manually building codecs. Manually codecs can be error prone because you have to be careful about the position of arguments which may not even be defined in the same file.

## Benchmark

This simple benchmark builds decoders for 200 case classes each with up to 30 random fields, which can be other nested case classes. This isnt quite like normal usage in a software projects but it is a good indication of the compilation performance. Im also comparing different scala versions 2.11 and 2.12 as they have different optimizations.

![chart](/assets/2017-07-06-circe-vs-argonaut.png)

As you can see Circe takes almost half the time ArgonautShapeless does! Circe's [semi auto](https://circe.github.io/circe/codec.html) derivation doesnt seem to provide much improvement over full auto. However in a normal project semi auto will help you organise code and make sure full auto isnt re calculating decoders.

## Conclusion
I think automatic derivation and scrap your boiler plate style programming is really good. But at some point as a project grows it can lead to large compile times. Maybe sometimes we should maintain some of that boiler plate and keep manually defined codecs. But I still dont want to write those, particularly once there are so many that the compilation time is slow.

Mabye we could use shapeless to export codec definations for us. Then we could switch from automatic to manuall without risk. But then as the project evolves they need to be updated, and if someone changes the defination of a case class by swapping the order of some fields it would introduce a bug that the compiler cant catch. You cant have that problem with automatic codecs. It could be caught by unit tests of the codecs, but those tests are more tedious error prone code to write.

## Further Work
If anyone is intrested I can include other json libraries.
