# Zuora 6 for 6 Modifier

This repo holds [a script](src/main/scala/com/gu/zuora6for6modifier/Main.scala) to do one of:
1. extend 6-for-6 Guardian Weekly introductory offers.
1. postpone 6-for-6 Guardian Weekly introductory offers where the offer begins in a week when Guardian Weekly
   isn't published.
1. postpone standard Guardian Weekly rate plans where the rate plan begins in a week when Guardian Weekly
   isn't published.

The specific use case is when an issue isn't published, which is usually in Christmas week.
In this case any subs that begin in the week of the unpublished issue have to be postponed to the
following week.
Any subs that begin in earlier weeks have to be extended so that the introductory offer
includes 6 published issues, as expected.

As the length of a rate plan cannot be modified in place, the script adds new rate plans to the
subscription with different start dates and lengths as appropriate.  Then the original introductory
and quarterly rate plans are removed.

## Configuration
The project expects an `application.conf` file in `src/main/resources`. Git will ignore this file.
It should contain these settings:
```hocon
zuora {
  stage = ???
  client_id = ???
  client_secret = ???
}
```
In the Prod stage there is a dedicated user configured, called `Christmas 6 for 6 extender`.
Use your own credentials for the other stages.

## Queries
The queries used to find relevant subscriptions in the BigQuery data lake are [here](queries).
