---
layout: post
title: generating sql test data
date: "2021-03-27T00:00:00.000-00:00"
author: Stephen Nancekivell
tags:
modified_time: "2021-03-27T00:00:00.000-00:00"
draft: true
---

- https://stackoverflow.com/questions/7398352/inserting-duplicated-mysql-test-data-the-easy-way
- https://stackoverflow.com/questions/32005009/is-there-an-easy-way-to-generate-test-data-in-mysql
- https://stackoverflow.com/questions/58748844/how-to-populate-mysql-database-with-test-data

```

set @@cte_max_recursion_depth = 10000000;
insert into user (created)
select
   from_unixtime(rand() * 1000000000)
 from (
  with recursive numbers as (
	select 1 as n
	union all
	select n + 1 from numbers
	where n < 1000000
  )
  select * from numbers
) as n;


insert into tweet(user_id, hash_tag)
select
  (floor(rand() * 1000000)+1),
   (floor(rand() * 1000)+1)
 from (
  with recursive numbers as (
	select 1 as n
	union all
	select n + 1 from numbers
	where n < 100000
  )
  select * from numbers
) as numbers;

```

```
explain  select o.* from object o
join object_item oi on oi.object_id = o.id
where oi.item_id = 10
order by o.created desc limit 10;
```

```
1	SIMPLE	oi	NULL	ref	object_id,object_item_1,object_item_2	object_item_2	5	const	105	100.00	Using index; Using temporary; Using filesort
1	SIMPLE	o	NULL	eq_ref	PRIMARY	PRIMARY	4	test.oi.object_id	1	100.00	NULL
```

```
create index object_item_2 on object_item(item_id, object_id);
explain  select o.* from object o
join object_item oi on oi.object_id = o.id
where oi.item_id = 10
order by oi.object_id desc limit 10;
```

```
1	SIMPLE	oi	NULL	ref	object_id,object_item_1,object_item_2	object_item_2	5	const	105	100.00	Backward index scan; Using index
1	SIMPLE	o	NULL	eq_ref	PRIMARY	PRIMARY	4	test.oi.object_id	1	100.00	NULL
```

create table user(
id int primary key auto_increment,  
 deleted tinyint default 0,
group_id int not null,
foreign key (group_id) references group(id),
);

create table user_group(
id int primary key auto_increment
account_id int not null,
foreign key (account_id) references account(id),
);

create table account(
id int primary key auto_increment
);

insert into account (id)
select
numbers.n
from (
with recursive numbers as (
select 1 as n
union all
select n + 1 from numbers
where n < 100
)
select \* from numbers
) as numbers

select \* from user
where deleted = 0
and group_id in (1,2,3,4,5)
order by id desc limit 10;

select u.\* from user u
join group g on u.group_id = g.id
where u.deleted = 0
and g.account_id = 85
order by id desc limit 10;
