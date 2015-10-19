package auction.system

import akka.actor._
import auction.system.AuctionCreated.{BidTimerExpired, StartAuction}
import auction.system.AuctionIgnored.{DeleteTimerExpired, Relist}
import auction.system.Bidding._
import auction.system.Buyer.{Bid => BuyerOffer}
import auction.system.Data._
import auction.system.States._
import auction.system.Timers.{BidTimer, DeleteTimer}

import scala.concurrent.duration._

/**
 * Created by novy on 18.10.15.
 */
class Auction extends LoggingFSM[AuctionState, AuctionData] {

  import context._

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(StartAuction(timers, params), Uninitialized) => startBidTimerAndBecomeCreated(timers, params)
  }

  when(Created) {
    case Event(BidTimerExpired, cfg@Config(timers, _)) =>
      startDeleteTimer(timers.deleteTimer)
      goto(Ignored) using cfg

    case Event(BuyerOffer(amount), cfg@Config(_, params)) if exceedsInitialValue(amount, params) =>
      notifyBuyerThatBidHasBeenAccepted(amount, sender())
      goto(Activated) using AuctionInProgress(cfg, List(Bid(amount, sender())))

    case Event(BuyerOffer(amount), cfg@Config(_, params)) if !exceedsInitialValue(amount, params) =>
      notifyAboutTooLowBid(amount, sender(), requiredNextBid(params))
      stay using cfg
  }

  when(Ignored) {
    case Event(DeleteTimerExpired, _) => stop()
    case Event(Relist, Config(timers, params)) => startBidTimerAndBecomeCreated(timers, params)
  }

  when(Activated) {
    case Event(BuyerOffer(amount), AuctionInProgress(cfg@Config(_, params), offers@topOffer :: _)) if exceedsOldBid(amount, topOffer, params) =>
      notifyBuyerThatBidHasBeenAccepted(amount, sender())
      notifyPreviousBuyerAboutBidTop(topOffer, amount, params)
      stay using AuctionInProgress(cfg, Bid(amount, sender()) :: offers)

    case Event(BuyerOffer(amount), data@AuctionInProgress(Config(_, params), topOffer :: _)) if !exceedsOldBid(amount, topOffer, params) =>
      notifyAboutTooLowBid(amount, sender(), requiredNextBid(params, Some(topOffer)))
      stay using data

    case Event(BidTimerExpired, AuctionInProgress(Config(timers, _), winner :: _)) =>
      startDeleteTimer(timers.deleteTimer)
      notifyWinner(winner)
      goto(Sold) using AuctionEnded(winner)
  }

  when(Sold) {
    case Event(DeleteTimerExpired, _) => stop()
  }

  private def startBidTimerAndBecomeCreated(timers: AuctionTimers, params: AuctionParams): Auction.this.State = {
    startBidTimer(timers.bidTimer)
    goto(Created) using Config(timers, params)
  }

  private def startBidTimer(bidTimer: BidTimer): Unit = system.scheduler.scheduleOnce(bidTimer.duration, self, BidTimerExpired)

  private def startDeleteTimer(timer: DeleteTimer): Unit = system.scheduler.scheduleOnce(timer.duration, self, DeleteTimerExpired)

  private def notifyWinner(highestOffer: Bid): Unit = {
    highestOffer.buyer ! AuctionWon(highestOffer.amount)
  }

  private def notifyBuyerThatBidHasBeenAccepted(amount: BigDecimal, buyer: ActorRef): Unit = {
    buyer ! BidAccepted(amount)
  }

  private def notifyAboutTooLowBid(amount: BigDecimal, buyer: ActorRef, expectedBid: BigDecimal): Unit = {
    buyer ! BidTooLow(amount, expectedBid)
  }


  private def notifyPreviousBuyerAboutBidTop(previousHighestOffer: Bid, newOfferValue: BigDecimal, params: AuctionParams): Unit = {
    previousHighestOffer.buyer ! BidTopBySomeoneElse(previousHighestOffer.amount, newOfferValue, params.step)
  }

  private def exceedsOldBid(newBidValue: BigDecimal, oldBid: Bid, auctionParams: AuctionParams): Boolean = {
    newBidValue >= requiredNextBid(auctionParams, Some(oldBid))
  }

  private def exceedsInitialValue(newBidValue: BigDecimal, auctionParams: AuctionParams): Boolean = {
    newBidValue >= requiredNextBid(auctionParams)
  }

  private def requiredNextBid(auctionParams: AuctionParams, highestOffer: Option[Bid] = None): BigDecimal = {
    highestOffer.map(offer => offer.amount + auctionParams.step).getOrElse(auctionParams.initialPrice)
  }
}

object Timers {

  case class BidTimer(duration: FiniteDuration)

  case object StartBidTimer

  case class DeleteTimer(duration: FiniteDuration)

  case object StartDeleteTimer

}

object AuctionCreated {

  case class StartAuction(timers: AuctionTimers, config: AuctionParams)

  case object BidTimerExpired

}

object AuctionIgnored {

  case object DeleteTimerExpired

  case object Relist

}

object Bidding {

  case class Bid(amount: BigDecimal, buyer: ActorRef)

  case class BidAccepted(amount: BigDecimal)

  case class BidTooLow(currentAmount: BigDecimal, requiredAmount: BigDecimal)

  case class BidTopBySomeoneElse(previousOffer: BigDecimal, currentHighestOffer: BigDecimal, requiredStep: BigDecimal)

  case class AuctionWon(winningOffer: BigDecimal)

}

object States {

  sealed trait AuctionState

  case object Idle extends AuctionState

  case object Created extends AuctionState

  case object Ignored extends AuctionState

  case object Activated extends AuctionState

  case object Sold extends AuctionState

}

object Data {

  case class AuctionParams(step: BigDecimal, initialPrice: BigDecimal)

  case class AuctionTimers(bidTimer: BidTimer, deleteTimer: DeleteTimer)


  sealed trait AuctionData

  case object Uninitialized extends AuctionData

  case class Config(timers: AuctionTimers, params: AuctionParams) extends AuctionData

  case class AuctionInProgress(config: Config, offers: List[Bid]) extends AuctionData

  case class AuctionEnded(winner: Bid) extends AuctionData

}