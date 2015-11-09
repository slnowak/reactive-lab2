package auction.system

import akka.actor.ActorRef

import scala.concurrent.duration.FiniteDuration

/**
 * Created by novy on 09.11.15.
 */
object Data {

  case class AuctionParams(step: BigDecimal, initialPrice: BigDecimal)

  case class BidTimer(duration: FiniteDuration)

  case class DeleteTimer(duration: FiniteDuration)

  case class AuctionTimers(bidTimer: BidTimer, deleteTimer: DeleteTimer)

  case class Bid(amount: BigDecimal, buyer: ActorRef)


  sealed trait AuctionData

  case object Uninitialized extends AuctionData

  case class Config(timers: AuctionTimers, params: AuctionParams, seller: ActorRef) extends AuctionData

  case class ConfigWithOffers(config: Config, offers: List[Bid]) extends AuctionData

  case class AuctionWinner(winner: Bid) extends AuctionData

}
