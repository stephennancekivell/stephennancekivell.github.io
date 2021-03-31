---
layout: post
title: Improve SQL Join Queries that use Temporary Tables & File Sorts
date: '2021-03-27T00:00:00.000-00:00'
author: Stephen Nancekivell
tags:
summary: Eliminate file sort for fast queries
image: /assets/2021-03-27-query-explain.png
modified_time: '2021-03-27T00:00:00.000-00:00'
---

Cross post with [Hands On Sql](https://handsonsql.com)

# Eliminate File Sort for Fast Queries

When SQL performs queries sometimes it needs to break down the work into intermediate steps saving the result, in order to do further filtering. It seems counterintuitive but it's often faster than if SQL were to do the join for each row 1 by 1. This is often the case with poorly indexed joins or inefficient layouts.

![query-explain](/assets/2021-03-27-query-explain.png)


When a query reaches a particular difficulty or size you will see `Using index; Using temporary; Using filesort` in the `explain` for the query. This can be particularly bad for large datasets where it can cause lots of disk IO. I've even seen it bring a server to its knees by exhausting the Amazon RDS IOPS credit. 


### Example
Consider the situation with two tables and a join query. We have `objects` and `items`. We are trying to list the recent objects with a particular item.

```sql
create table object(
  id int primary key auto_increment,  
  created datetime ,
  index object_1 (created)
);

create table object_item(
   id int primary key auto_increment,
   item_id int, -- some other item type
   object_id int not null,
   foreign key (object_id) references object(id),
   index object_item_1 (item_id)
);
```

The query would be like this.
```sql
select o.* from object o
join object_item oi on oi.object_id = o.id
where oi.item_id = @particular_item
order by o.created desc limit 10;
```

In the `explain` for the query we see the dreaded `Using index; Using temporary; Using filesort`. So its going to be slow.

### The Reason
This is because the data is being filtered in one table and sorted with the other. We only need the last 10 `objects` but it needs to filter all of the `object_item`'s and join them to find out what to sort.

If we could have an index across both tables or have the filter and sort on the same table then we can get that sweet `Backward index scan; Using index` performance. 

In some databases like Postgres you can use materialized views to achieve this. In others you need to **change the data** or **change the query** to make it easier for SQL.

By changing the data we need to copy the `created` field to the `order_time` table.

So we can write a query like this.
```sql
select o.* from object o
join object_item oi on oi.object_id = o.id
where oi.item_id = @particular_item
order by oi.created desc limit 10;
```

It is annoying that you need two copies of the created time field. One way to keep the field up to date would be with triggers.

Luckily there is another property we can use to make the query easier. Because our database is using incrementing primary keys we can use the `object_id` as a proxy for `created` in the sort.

For example
```sql
select o.* from object o
join object_item oi on oi.object_id = o.id
where oi.item_id = @particular_item
order by oi.object_id desc limit 10;
```


Now the optimized query can run 100s or 1000s of times faster, going from minutes to milliseconds.
