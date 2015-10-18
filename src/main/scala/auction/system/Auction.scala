package auction.system

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.system.Auction.{Bid, BidAccepted, BidTooLow}

/**
 * Created by novy on 18.10.15.
 */
class Auction(step: BigDecimal) extends Actor {

  private var highestOffer: Option[Bid] = None

  override def receive: Receive = LoggingReceive {
    case Bid(amount) => handleNewOffer(amount, sender())
  }

  private def handleNewOffer(newBid: BigDecimal, buyer: ActorRef): Unit = {
    if (exceedsOldBid(newBid)) {
      highestOffer = Some(Bid(newBid, buyer))
      buyer ! BidAccepted(newBid)
    } else {
      buyer ! BidTooLow(newBid, requiredNextBid())
    }
  }

  private def exceedsOldBid(newBid: BigDecimal): Boolean = {
    newBid >= requiredNextBid()
  }

  private def requiredNextBid(): BigDecimal = {
    highestOffer.map(_.amount).getOrElse(BigDecimal(0)) + step
  }
}

object Auction {

  case class Bid(amount: BigDecimal, buyer: ActorRef)

  case class BidAccepted(amount: BigDecimal)

  case class BidTooLow(currentAmount: BigDecimal, requiredAmount: BigDecimal)

  case class BidTopBySomeoneElse(previousOffer: BigDecimal, currentHighestOffer: BigDecimal, requiredStep: BigDecimal)

  case class AuctionWon(winningOffer: BigDecimal)

}