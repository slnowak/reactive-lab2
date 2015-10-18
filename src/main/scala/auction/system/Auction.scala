package auction.system

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import auction.system.Auction.{Bid, BidAccepted, BidTooLow, BidTopBySomeoneElse}
import auction.system.Buyer.{Bid => BuyerOffer}

/**
 * Created by novy on 18.10.15.
 */
class Auction(step: BigDecimal, initialPrice: BigDecimal) extends Actor {

  private var highestOffer: Option[Bid] = None

  override def receive: Receive = LoggingReceive {
    case BuyerOffer(amount) => handleNewOffer(amount, sender())
  }

  private def handleNewOffer(newBid: BigDecimal, buyer: ActorRef): Unit = {
    if (exceedsOldBid(newBid)) {
      val previousHighestOffer = highestOffer
      highestOffer = Some(Bid(newBid, buyer))
      buyer ! BidAccepted(newBid)
      previousHighestOffer foreach notifyPreviousBuyerAboutBidTop(newBid)
    } else {
      buyer ! BidTooLow(newBid, requiredNextBid())
    }
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

object Auction {

  case class Bid(amount: BigDecimal, buyer: ActorRef)

  case class BidAccepted(amount: BigDecimal)

  case class BidTooLow(currentAmount: BigDecimal, requiredAmount: BigDecimal)

  case class BidTopBySomeoneElse(previousOffer: BigDecimal, currentHighestOffer: BigDecimal, requiredStep: BigDecimal)

  case class AuctionWon(winningOffer: BigDecimal)

  def props(step: BigDecimal, initialPrice: BigDecimal): Props = Props(new Auction(step, initialPrice))
}