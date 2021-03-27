

```

set @@cte_max_recursion_depth = 10000000;
insert into object (created)
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


insert into object_item(object_id, item_id)
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