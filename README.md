# Zuora 6 for 6 Modifier

This repo holds [a script](src/main/scala/com/gu/zuora6for6modifier/Main.scala) to either extend or
postpone 6-for-6 Guardian Weekly introductory offers.  

The specific use case is when an issue isn't published, which is usually in Christmas week.
In this case any subs that begin in the week of the unpublished issue have to be postponed to the
following week.
Any subs that begin in earlier weeks have to be extended so that the introductory offer
includes 6 published issues, as expected.

As the length of a rate plan cannot be modified in place, the script adds new rate plans to the
subscription with different start dates and lengths as appropriate.  Then the original introductory
and quarterly rate plans are removed.
