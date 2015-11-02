package auction.system

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction.system.AuctionSearch.{QueryResult, Registered, Unregistered}
import auction.system.Buyer.FindAuctions
import auction.system.Seller.{AuctionRef, Register, Unregister}

/**
 * Created by novy on 02.11.15.
 */
class AuctionSearch extends Actor {

  private var registeredAuctions: Set[AuctionRef] = Set()

  override def receive: Receive = LoggingReceive {
    case Register(auctionRef) => register(auctionRef, sender())
    case Unregister(auctionRef) => unregister(auctionRef, sender())
    case FindAuctions(keyword) => findMatchingAuctions(keyword, sender())
  }

  private def register(auctionRef: AuctionRef, seller: ActorRef): Unit = {
    registeredAuctions = registeredAuctions + auctionRef
    seller ! Registered(auctionRef)
  }

  private def unregister(auctionRef: AuctionRef, seller: ActorRef): Unit = {
    registeredAuctions = registeredAuctions - auctionRef
    seller ! Unregistered(auctionRef)
  }

  private def findMatchingAuctions(keyword: String, buyer: ActorRef): Unit = {
    def matchesKeyword(keyword: String): AuctionRef => Boolean =
      auction => auction.title.split(' ').contains(keyword)

    val matchingAuctions = registeredAuctions.filter(matchesKeyword(keyword))

    buyer ! QueryResult(keyword, matchingAuctions)
  }
}

case object AuctionSearch {

  case class QueryResult(keyword: String, auctions: Set[AuctionRef])

  case class Registered(auction: AuctionRef)

  case class Unregistered(auction: AuctionRef)

}


