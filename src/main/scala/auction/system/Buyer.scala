package auction.system

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.system.Auction.{AuctionWon, BidAccepted, BidTooLow, BidTopBySomeoneElse}
import auction.system.Buyer.{Bid, SentBid, StartBidding, WonAuction}

/**
 * Created by novy on 18.10.15.
 */
class Buyer(var moneyToSpend: BigDecimal) extends Actor {

  private var wonAuctions: Set[WonAuction] = Set()
  private var offers: Set[SentBid] = Set()

  override def receive: Receive = LoggingReceive {
    case StartBidding(initialBid, auction) => sendBidIfCanAfford(initialBid, auction)
    case BidAccepted => _
    case BidTooLow(offered, expected) => tryToTopBid(offered, expected, sender())
    case BidTopBySomeoneElse(previous, actualHighest, step) => handleBidTopBySomeoneElse(previous, actualHighest, step, sender())
    case AuctionWon(winningOffer) => handleAuctionWon(winningOffer, sender())
  }

  private def tryToTopBid(previousOffer: BigDecimal, expectedOffer: BigDecimal, auction: ActorRef): Unit = {
    offers = offers - SentBid(previousOffer, auction)

    sendBidIfCanAfford(expectedOffer, auction)
  }

  private def sendBidIfCanAfford(offer: BigDecimal, auction: ActorRef): Unit = {
    if (canAfford(offer)) {
      offers = offers + SentBid(offer, auction)
      auction ! Bid(offer)
    }
  }

  private def handleAuctionWon(winningOffer: BigDecimal, auction: ActorRef): Unit = {
    offers = offers - SentBid(winningOffer, auction)
    moneyToSpend -= winningOffer
    wonAuctions = wonAuctions + WonAuction(winningOffer, auction)
  }

  private def handleBidTopBySomeoneElse(previous: BigDecimal, actualHighest: BigDecimal, requiredStep: BigDecimal, auction: ActorRef): Unit = {
    tryToTopBid(previous, actualHighest + requiredStep, auction)
  }

  private def canAfford(offer: BigDecimal): Boolean = {
    val totalOffers: BigDecimal = offers
      .map(_.amount)
      .foldLeft(BigDecimal(0))(_ + _)

    moneyToSpend >= offer + totalOffers
  }
}

object Buyer {

  case class StartBidding(amount: BigDecimal, auction: ActorRef)

  case class SentBid(amount: BigDecimal, auction: ActorRef)

  case class Bid(amount: BigDecimal)

  case class WonAuction(amount: BigDecimal, auction: ActorRef)

}
