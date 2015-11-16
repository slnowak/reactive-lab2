package auction.system

import akka.actor.ActorRef
import auction.system.Data.{AuctionParams, AuctionTimers, Bid, WithConfig}

/**
 * Created by novy on 09.11.15.
 */
object AuctionEvents {

  sealed trait AuctionEvent

  case class AuctionCreatedEvent(timers: AuctionTimers, params: AuctionParams, seller: ActorRef) extends AuctionEvent

  case object BidTimerExpiredEvent extends AuctionEvent

  case class AuctionActivatedEvent(config: WithConfig, initialOffer: Bid) extends AuctionEvent

  case class NewHighestOfferArrivedEvent(config: WithConfig, newOffer: Bid) extends AuctionEvent

  case class AuctionEndedEvent(config: WithConfig, winner: Bid) extends AuctionEvent

}
