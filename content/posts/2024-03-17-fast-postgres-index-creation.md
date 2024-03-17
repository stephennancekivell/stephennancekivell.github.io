---
layout: post
title: "Fast postgres index creation"
date: "2024-03-17T00:00:00.000"
author: Stephen Nancekivell
tags:
cover:
  image: /assets/2024-03-17-elephants-sorting.jpeg
  hidden: true
---

# Fast postgres index creation

Postgres is a fantastic database for a multitude of reasons, celebrated for its reliability and flexibility. However, when creating indexes on large tables, it can sometimes feel sluggish. This becomes even more noticeable when your project requires the creation of numerous indexes, potentially slowing down your progress.

![Elephants sorting!](/assets/2024-03-17-elephants-sorting.jpeg)


## Configure CPU and RAM available for index creation

There are two settings that control the amount of server resources available for index creation, `maintenance_work_mem` and `max_parallel_maintenance_workers`. The defaults for these were set in the time of the dinausours and are 64MB and 2 cores.

You can adjust these settings temporarily for your session by using `set`. Eg

```sql
-- set for the current session
set maintenance_work_mem='8GB';
set max_parallel_maintenance_workers=8;
create index my_index_abc on table(col_1, col_2);
```

Or you can define these in the postgres config file, or however your cloud lets you configure it.

## Monitoring progress

Ever wondered how your index creation is faring? Postgres offers a handy way to monitor the progress in real-time in `pg_stat_progress_create_index`. With the following query, you can get a snapshot of how much work has been done and what's left:

```sql
SELECT
  age(clock_timestamp(), query_start),
  a.query,
  p.phase,
  round(p.blocks_done / NULLIF(p.blocks_total::numeric,0) * 100, 2) AS "% done",
  p.blocks_total,
  p.blocks_done,
  round(p.tuples_done / NULLIF(p.tuples_total::numeric,0) * 100, 2) AS "% tuples done",
  p.tuples_total,
  p.tuples_done,
FROM pg_stat_progress_create_index p
JOIN pg_stat_activity a ON p.pid = a.pid
LEFT JOIN pg_stat_all_indexes ai on ai.relid = p.relid AND ai.indexrelid = p.index_relid;
```

There are up to 10 phases it has to go through, but most of the time will be spend it `buliding indexes` then `index validation`.

## Streamlining Index Creation


If you have lots of indexes to create you probably want to run them sequentually, but if each one takes an hour or more to create you dont want to have to manually start each one.

You can issue all of your statements in a single statement so that it automatically runs the next when the first is finished, even if your connection is broken.

```sql
-- create indexes in a single transaction
create index my_index_a on table(col_1);
create index my_index_b on table(col_2);
```

The downside of this is that it will be hard to track progress, and if there is an error creating one index they will all be rolled back. Instead you can create a new transaction for each index by wrapping the `create index` statements in `begin` and `commit`.
```sql
begin;
create index my_index_a on table(col_1);
commit;

begin;
create index my_index_b on table(col_2);
commit;
```


## Wrapping Up

I hope these tips help when you need to create your next batch of big indexes.
