---
layout: post
title: Docker Repository Cleaning
date: "2021-04-12T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
summary: How to clean your Docker Repository
image: /assets/2021-04-12-compile-summary-chart-2.png
modified_time: "2021-04-17T00:00:00.000-00:00"
---

# Docker Repository Cleaning

Your [Docker](https://docker.com) Repositories can become full of old images that you don’t need anymore. Left unchecked these images can take up considerable space and cost a considerable amount of money. When deploying images with continuous integration this cost can explode.

## Cost Calculator

<iframe style="width:40rem;height:12rem;" src="https://docs.google.com/spreadsheets/d/e/2PACX-1vTTZthQ_Z05MENjSomeemEzMzFYk2P-f47H91TFIUvCfy7iZQMwpb4FsTyNlGIyRCrKKskpSXiOoH1a/pubhtml?gid=0&amp;single=true&amp;widget=true&amp;headers=false"></iframe>

## Deleting Risk

Deleting images comes with some risk. If there are containers using an image, and it is deleted from the repository, you may not be able to recreate the container. When using [Kubernetes](http://kubernetes.io/) or [AWS ECS](https://aws.amazon.com/ecs) containers can be rebuilt during cluster maintenance or auto scaling.

## No Docker Command to Delete

In Docker there is no native way to delete a tag from a repository. This is good for public image or source code repositories. If lots of things are depending on another and it gets deleted, all of the users will have broken dependency. This is what happened with the [npm left-pad debacle](https://qz.com/646467/how-one-programmer-broke-the-internet-by-deleting-a-tiny-piece-of-code/).

But for your private docker repositories where you control all of the usages of an image, you want to be able to delete old images.

## How to Safely Delete Docker Images

- Dont delete images that are in use or that you might need soon. Often it is ok to assume that only the most recent will be needed.
- Keep the last 5 snapshot builds
- Keep the last 10 non-snapshot builds
- Keep the “latest” tag
- Delete anything else

## AWS Lifecycle Rules

AWS provides lifecycle rules which can delete old images, but it does not have the smarts to check if an image is still in use by a running container or if it is a SNAPSHOT or latest build. So you are left with the assumption that only the last N builds are important and crossing your fingers.

## AWS ECR Cleaning Script

You can build a bash simple script using the `aws cli` command to delete old images. You can filter for SNAPSHOT and latest. However this is still using the assumption that only the most recent are important.

```bash
#!/usr/bin/env bash

REPOSITORY="repo"
MAX_SNAPSHOTS=5
MAX_NON_SNAPSHOTS=10

images=$(aws ecr describe-images --repository-name "$REPOSITORY")
sortedImageTags=$(echo "$images" | jq -r '.imageDetails | sort_by(.imagePushedAt) | reverse | .[].imageTags[]')
snapshotsToDelete=$(echo "$sortedImageTags" | grep SNAPSHOT | tail -n +$MAX_SNAPSHOTS)
nonSnapshotsToDelete=$(echo "$sortedImageTags" | grep -v "SNAPSHOT" | grep -v "latest" | tail -n +$MAX_NON_SNAPSHOTS)
allToDelete=$(echo "$snapshotsToDelete $nonSnapshotsToDelete" | sed 's/.*/imageTag=&/' | tr '\n' ' ')
aws ecr bulk-delete-images --repository-name $REPOSITORY "$allToDelete"
```

Happy Hacking
