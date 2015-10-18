package auction.system

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.system.Auction.{AuctionWon, BidAccepted, BidTooLow, BidTopBySomeoneElse}
import auction.system.Buyer.{Bid, SentBid, WonAuction}

/**
 * Created by novy on 18.10.15.
 */
class Buyer(var moneyToSpend: BigDecimal) extends Actor {

  private var wonAuctions: Set[WonAuction] = Set()
  private var offers: Set[SentBid] = Set()


  override def receive: Receive = LoggingReceive {
    case BidAccepted => _
    case BidTooLow(offered, expected) => tryToTopBid(offered, expected, sender())
    case AuctionWon(winningOffer) => handleAuctionWon(winningOffer, sender())
    case BidTopBySomeoneElse(previous, actualHighest, step) => handleBidTopBySomeoneElse(previous, actualHighest, step, sender())
  }


  def tryToTopBid(previousOffer: BigDecimal, expectedOffer: BigDecimal, auction: ActorRef): Unit = {
    offers = offers - SentBid(previousOffer, auction)

    if (canAfford(expectedOffer)) {
      offers = offers + SentBid(expectedOffer, auction)
      auction ! Bid(expectedOffer)
    }
  }

  def handleAuctionWon(winningOffer: BigDecimal, auction: ActorRef): Unit = {
    offers = offers - SentBid(winningOffer, auction)
    moneyToSpend -= winningOffer
    wonAuctions = wonAuctions + WonAuction(winningOffer, auction)
  }

  def handleBidTopBySomeoneElse(previous: BigDecimal, actualHighest: BigDecimal, requiredStep: BigDecimal, auction: ActorRef): Unit = {
    tryToTopBid(previous, actualHighest + requiredStep, auction)
  }
  
  def canAfford(offer: BigDecimal): Boolean = {
    val totalOffers: BigDecimal = offers
      .map(_.amount)
      .foldLeft(BigDecimal(0))(_ + _)

    moneyToSpend >= offer + totalOffers
  }
}

object Buyer {

  case class SentBid(amount: BigDecimal, action: ActorRef)

  case class Bid(amount: BigDecimal) {
    require(amount > 0)
  }

  case class WonAuction(amount: BigDecimal, auction: ActorRef)
}
