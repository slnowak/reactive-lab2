package auction.system

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import auction.system.Auction._
import auction.system.AuctionCreated.{BidTimerExpired, StartBidTimer}
import auction.system.AuctionIgnored.{DeleteTimerExpired, Relist}
import auction.system.Buyer.{Bid => BuyerOffer}
import auction.system.Timers.{BidTimer, DeleteTimer}

import scala.concurrent.duration._

/**
 * Created by novy on 18.10.15.
 */
class Auction(bidTimer: BidTimer,
              deleteTimer: DeleteTimer,
              step: BigDecimal,
              initialPrice: BigDecimal) extends Actor {

  import context._

  private var highestOffer: Option[Bid] = None

  override def receive: Receive = LoggingReceive {
    case StartBidTimer => startAuction()
  }

  private def startAuction(): Unit = {
    startBidTimer()
    become(created)
  }

  private def startBidTimer(): Unit = system.scheduler.scheduleOnce(bidTimer.duration, self, BidTimerExpired)

  def created: Receive = LoggingReceive {
    case BidTimerExpired =>
      startDeleteTimer()
      become(ignored)

    case BuyerOffer(amount) =>
      handleNewOffer(amount, sender()) foreach (_ => become(activated))
  }

  private def startDeleteTimer(): Unit = system.scheduler.scheduleOnce(deleteTimer.duration, self, DeleteTimerExpired)

  def ignored: Receive = LoggingReceive {
    case DeleteTimerExpired => stop(self)
    case Relist => startAuction()
  }

  def activated: Receive = LoggingReceive {
    case BuyerOffer(amount) => handleNewOffer(amount, sender())
    case BidTimerExpired =>
      startDeleteTimer()
      notifyBuyer()
      become(sold)
  }

  private def notifyBuyer(): Unit = {
    highestOffer foreach (offer => offer.buyer ! AuctionWon(offer.amount))
  }

  def sold: Receive = LoggingReceive {
    case DeleteTimerExpired => stop(self)
  }


  private def handleNewOffer(newBid: BigDecimal, buyer: ActorRef): Option[Bid] = {
    if (exceedsOldBid(newBid)) {
      val previousHighestOffer = highestOffer
      highestOffer = Some(Bid(newBid, buyer))
      buyer ! BidAccepted(newBid)
      previousHighestOffer foreach notifyPreviousBuyerAboutBidTop(newBid)
      return highestOffer
    }

    buyer ! BidTooLow(newBid, requiredNextBid())
    None
  }

  private def notifyPreviousBuyerAboutBidTop(newOffer: BigDecimal)(previousHighestOffer: Bid): Unit = {
    previousHighestOffer.buyer ! BidTopBySomeoneElse(previousHighestOffer.amount, newOffer, step)
  }

  private def exceedsOldBid(newBid: BigDecimal): Boolean = {
    newBid >= requiredNextBid()
  }

  private def requiredNextBid(): BigDecimal = {
    highestOffer.map(offer => offer.amount + step).getOrElse(initialPrice)
  }
}

object Timers {

  case class BidTimer(duration: FiniteDuration)

  case class DeleteTimer(duration: FiniteDuration)

}

object AuctionCreated {

  case object StartBidTimer

  case object BidTimerExpired

}

object AuctionIgnored {

  case object StartDeleteTimer

  case object DeleteTimerExpired

  case object Relist

}


object Auction {

  case class Bid(amount: BigDecimal, buyer: ActorRef)

  case class BidAccepted(amount: BigDecimal)

  case class BidTooLow(currentAmount: BigDecimal, requiredAmount: BigDecimal)

  case class BidTopBySomeoneElse(previousOffer: BigDecimal, currentHighestOffer: BigDecimal, requiredStep: BigDecimal)

  case class AuctionWon(winningOffer: BigDecimal)

  def props(step: BigDecimal,
            initialPrice: BigDecimal,
            bidTimer: BidTimer = BidTimer(30 seconds),
            deleteTimer: DeleteTimer = DeleteTimer(45 seconds)): Props = {

    Props(new Auction(bidTimer, deleteTimer, step, initialPrice))
  }
}