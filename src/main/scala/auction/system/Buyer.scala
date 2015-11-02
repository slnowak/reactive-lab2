package auction.system

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import auction.system.AuctionSearch.QueryResult
import auction.system.Bidding.{AuctionWon, BidAccepted, BidTooLow, BidTopBySomeoneElse}
import auction.system.Buyer._
import auction.system.Seller.AuctionRef

/**
 * Created by novy on 18.10.15.
 */
class Buyer(auctionSearch: ActorRef, var moneyToSpend: BigDecimal, keyword: String) extends Actor {

  private var wonAuctions: Set[WonAuction] = Set()
  private var offers: Set[SentBid] = Set()

  private var initialBid: BigDecimal = _


  override def receive: Receive = LoggingReceive {
    case StartBidding(withInitial) =>
      initialBid = withInitial
      askForAuctions(keyword)
    case QueryResult(_, auctions) => sendInitialIfCanAfford(auctions)
    case BidAccepted =>
    case BidTooLow(offered, expected) => tryToTopBid(offered, expected, sender())
    case BidTopBySomeoneElse(previous, actualHighest, step) => tryToTopBid(previous, actualHighest + step, sender())
    case AuctionWon(winningOffer) => handleAuctionWon(winningOffer, sender())
  }

  def askForAuctions(keyword: String): Unit = auctionSearch ! FindAuctions(keyword)

  private def tryToTopBid(previousOffer: BigDecimal, expectedOffer: BigDecimal, auction: ActorRef): Unit = {
    offers = offers - SentBid(previousOffer, auction)

    sendBidIfCanAfford(expectedOffer, auction)
  }

  private def sendInitialIfCanAfford(auctions: Set[AuctionRef]): Unit = {
    def sendInitialBidIfCanAfford(auctionRef: AuctionRef): Unit =
      sendBidIfCanAfford(initialBid, auctionRef.auction)

    auctions foreach sendInitialBidIfCanAfford
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

  private def canAfford(offer: BigDecimal): Boolean = {
    val totalOffers = offers.toList.map(_.amount).sum

    moneyToSpend >= offer + totalOffers
  }
}

object Buyer {

  case class StartBidding(amount: BigDecimal)

  case class SentBid(amount: BigDecimal, auction: ActorRef)

  case class Bid(amount: BigDecimal)

  case class FindAuctions(keyword: String)

  case class WonAuction(amount: BigDecimal, auction: ActorRef)

  def props(auctionSearch: ActorRef, moneyToSpend: BigDecimal, keyword: String): Props =
    Props(new Buyer(auctionSearch, moneyToSpend, keyword))
}
