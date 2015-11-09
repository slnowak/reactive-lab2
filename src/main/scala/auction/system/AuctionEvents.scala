package auction.system

import akka.actor.ActorRef
import auction.system.Data.{AuctionParams, AuctionTimers, Bid, Config}

/**
 * Created by novy on 09.11.15.
 */
object AuctionEvents {

  sealed trait AuctionEvent

  case class AuctionCreated(timers: AuctionTimers, params: AuctionParams, seller: ActorRef) extends AuctionEvent

  case class AuctionActivated(config: Config, initialOffer: Bid) extends AuctionEvent

  case class NewHighestOfferArrived(config: Config, newOffer: Bid) extends AuctionEvent

  case class AuctionEnded(winner: Bid) extends AuctionEvent

}
