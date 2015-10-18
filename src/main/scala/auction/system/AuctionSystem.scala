package auction.system

import akka.actor.ActorSystem

/**
 * Created by novy on 18.10.15.
 */
object AuctionSystem extends App {
  private val system: ActorSystem = ActorSystem("auction-system")

  system.awaitTermination()
}
