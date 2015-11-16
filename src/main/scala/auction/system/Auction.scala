package auction.system

import java.time.LocalDateTime
import java.util.UUID

import akka.actor._
import akka.persistence.fsm.PersistentFSM
import auction.system.AuctionCreatedMoveMe.{BidTimerExpired, StartAuction}
import auction.system.AuctionEvents._
import auction.system.AuctionIgnored.{DeleteTimerExpired, Relist}
import auction.system.AuctionStates._
import auction.system.Bidding._
import auction.system.Buyer.{Bid => BuyerOffer}
import auction.system.Data._

import scala.reflect._

/**
  * Created by novy on 18.10.15.
  */
class Auction(auctionId: String) extends PersistentFSM[AuctionState, AuctionData, AuctionEvent] {

  import context._

  override def persistenceId: String = s"fsm-auction-$auctionId"

  override def domainEventClassTag: ClassTag[AuctionEvent] = classTag[AuctionEvent]

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(StartAuction(timers, params), Uninitialized) =>
      startBidTimer(timers.bidTimer)
      goto(Created) applying AuctionCreatedEvent(timers, params, sender())
  }

  when(Created) {
    case Event(BidTimerExpired, cfg@WithConfig(timers, _, seller)) =>
      notifySellerThereWasNoOffers(seller)
      startDeleteTimer(timers.deleteTimer)
      goto(Ignored) applying BidTimerExpiredEvent

    case Event(BuyerOffer(amount), cfg@WithConfig(_, params, _)) if exceedsInitialValue(amount, params) =>
      notifyBuyerThatBidHasBeenAccepted(amount, sender())
      goto(Activated) applying AuctionActivatedEvent(cfg, Bid(amount, sender()))

    case Event(BuyerOffer(amount), cfg@WithConfig(_, params, _)) if !exceedsInitialValue(amount, params) =>
      notifyAboutTooLowBid(amount, sender(), requiredNextBid(params))
      stay()
  }

  when(Ignored) {
    case Event(DeleteTimerExpired, _) => stop()
    case Event(Relist(relistAt), WithConfig(timers, AuctionParams(step, initialPrice, _), seller)) =>
      startBidTimer(timers.bidTimer)
      goto(Created) applying AuctionCreatedEvent(timers, AuctionParams(step, initialPrice, relistAt), sender())
  }

  when(Activated) {
    case Event(BuyerOffer(amount), WithConfigAndOffers(cfg@WithConfig(_, params, _), offers@topOffer :: _)) if exceedsOldBid(amount, topOffer, params) =>
      notifyBuyerThatBidHasBeenAccepted(amount, sender())
      notifyPreviousBuyerAboutBidTop(topOffer, amount, params)
      stay applying NewHighestOfferArrivedEvent(cfg, Bid(amount, sender()))

    case Event(BuyerOffer(amount), data@WithConfigAndOffers(WithConfig(_, params, _), topOffer :: _)) if !exceedsOldBid(amount, topOffer, params) =>
      notifyAboutTooLowBid(amount, sender(), requiredNextBid(params, Some(topOffer)))
      stay()

    case Event(BidTimerExpired, WithConfigAndOffers(cfg@WithConfig(timers, _, seller), winner :: _)) =>
      startDeleteTimer(timers.deleteTimer)
      notifySellerAboutWinner(seller, winner)
      notifyWinner(winner)
      goto(Sold) applying AuctionEndedEvent(cfg, winner)
  }

  when(Sold) {
    case Event(DeleteTimerExpired, _) => stop()
  }

  override def applyEvent(domainEvent: AuctionEvent, currentData: AuctionData): AuctionData = {
    domainEvent match {
      case AuctionCreatedEvent(timers, params, seller) => WithConfig(timers, params, seller)
      case BidTimerExpiredEvent => currentData
      case AuctionActivatedEvent(cfg, initialOffer) => WithConfigAndOffers(cfg, List(initialOffer))
      case NewHighestOfferArrivedEvent(cfg, newOffer) =>
        val offers = currentData.asInstanceOf[WithConfigAndOffers].offers
        WithConfigAndOffers(cfg, newOffer :: offers)
      case AuctionEndedEvent(cfg, winner) => WithAuctionWinner(cfg, winner)
    }
  }

  override def onRecoveryCompleted(): Unit = {
    super.onRecoveryCompleted()

    val now: LocalDateTime = LocalDateTime.now()
    stateName match {
      case _: AlreadyCrated => restoreTimers(now)
      case _ =>
    }
  }

  private def restoreTimers(now: LocalDateTime): Unit = {
    stateData match {
      case Uninitialized =>
      case alreadyInitialized: Initialized =>
        val timers: AuctionTimers = alreadyInitialized.timers
        val bidTimerExpired: Boolean =
          now.isAfter(alreadyInitialized.startedAt.plusNanos(timers.bidTimer.duration.toNanos))

        // todo shouldn't we subtract expired time?
        if (bidTimerExpired) startDeleteTimer(timers.deleteTimer) else startBidTimer(timers.bidTimer)
    }
  }

  private def notifySellerThereWasNoOffers(seller: ActorRef): Unit = {
    seller ! NoOffers
  }

  private def notifySellerAboutWinner(seller: ActorRef, winner: Bid) = {
    seller ! AuctionWonBy(winner.buyer, winner.amount)
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

object AuctionCreatedMoveMe {

  case class StartAuction(timers: AuctionTimers, config: AuctionParams)

  case object BidTimerExpired

}

object AuctionIgnored {

  case object DeleteTimerExpired

  case class Relist(relistAt: LocalDateTime = LocalDateTime.now())

}

object Bidding {

  case class BidAccepted(amount: BigDecimal)

  case class BidTooLow(currentAmount: BigDecimal, requiredAmount: BigDecimal)

  case class BidTopBySomeoneElse(previousOffer: BigDecimal, currentHighestOffer: BigDecimal, requiredStep: BigDecimal)

  case class AuctionWon(winningOffer: BigDecimal)

  case class AuctionWonBy(winner: ActorRef, winningOffer: BigDecimal)

  case object NoOffers

}

object Auction {
  def props(auctionId: String = UUID.randomUUID().toString): Props = Props(new Auction(auctionId))
}