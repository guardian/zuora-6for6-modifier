-- Find GW subscriptions whose introductory period includes Christmas week so that they can be extended by a week.
select c.subscription_id, c.name, s.term_start_date, c.effective_start_date, c.effective_end_date
from datalake.zuora_rateplancharge c,
     datalake.zuora_subscription s
where c.subscription_id = s.id
  and s.status = 'Active'
  and c.product_name like 'Guardian Weekly%'
  and (c.name like '%6%' or lower(c.name) like '%six%')
  and c.effective_start_date < '2020-12-25'
  and c.effective_end_date > '2020-12-25'
