-- Find GW subscriptions whose introductory period begins in Christmas week so that they can be pushed back by a week.
select c.subscription_id, c.name, s.term_start_date
from datalake.zuora_rateplancharge c,
     datalake.zuora_subscription s
where c.subscription_id = s.id
  and s.status = 'Active'
  and c.product_name like 'Guardian Weekly%'
  and (c.name like '%6%' or lower(c.name) like '%six%')
  and c.effective_start_date = '2020-12-25'
