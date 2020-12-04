package com.gu.zuora6for6modifier

import zio.console.{Console, putStrLn}
import zio.{App, ZEnv, ZIO}

object Main extends App {

  /** @param args  valid values of arg(0) are:<ul>
    *              <li>"extend" to extend the 6-for-6 period so that it includes at least 6 issues.<br />
    *               In this case a file called "extend.in.txt" will be expected in the root
    *               of the project holding a list of subscription names, one on each line.</li>
    *              <li>"postpone" to postpone the start of the 6-for-6 period by a week<br />
    *               In this case a file called "postpone.in.txt" will be expected in the root
    *               of the project holding a list of subscription names, one on each line.</li></ul>
    *              In both cases the start of the subsequent quarterly plan is postponed
    *              until the new end of the introductory plan.
    */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val action = args.headOption
    val process = action match {
      case Some("extend")   => Extender.extendSubscriptions
      case Some("postpone") => Postponer.postponeSubscriptions
      case _                => ZIO.dieMessage("No identifying action given")
    }
    process
      .provide(new ZuoraLive with Console.Live)
      .foldM(
        e => putStrLn(e.getMessage).map(_ => 1),
        _ => ZIO.succeed(0)
      )
  }
}
