package auction.system

import java.time.LocalDateTime

import akka.actor.ActorRef

import scala.concurrent.duration.FiniteDuration

/**
  * Created by novy on 09.11.15.
  */
object Data {

  case class AuctionParams(step: BigDecimal, initialPrice: BigDecimal, startingAt: LocalDateTime = LocalDateTime.now())

  case class BidTimer(duration: FiniteDuration)

  case class DeleteTimer(duration: FiniteDuration)

  case class AuctionTimers(bidTimer: BidTimer, deleteTimer: DeleteTimer)

  case class Bid(amount: BigDecimal, buyer: ActorRef)


  sealed trait AuctionData

  sealed trait Initialized extends AuctionData {
    def startedAt: LocalDateTime

    def timers: AuctionTimers
  }

  case object Uninitialized extends AuctionData

  case class WithConfig(timers: AuctionTimers, params: AuctionParams, seller: ActorRef) extends Initialized {
    override def startedAt: LocalDateTime = params.startingAt
  }

  case class WithConfigAndOffers(config: WithConfig, offers: List[Bid]) extends Initialized {
    override def startedAt: LocalDateTime = config.startedAt

    override def timers: AuctionTimers = config.timers
  }

  case class WithAuctionWinner(config: WithConfig, winner: Bid) extends Initialized {
    override def startedAt: LocalDateTime = config.startedAt

    override def timers: AuctionTimers = config.timers
  }

}
