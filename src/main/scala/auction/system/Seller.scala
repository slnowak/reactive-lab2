package auction.system

import akka.actor.{Props, Actor, ActorRef}
import auction.system.AuctionCoordinator.Start
import auction.system.AuctionCreated.StartAuction
import auction.system.AuctionSearch.{Registered, Unregistered}
import auction.system.Bidding.{AuctionWonBy, NoOffers}
import auction.system.Data.{AuctionParams, AuctionTimers}
import auction.system.Seller._

/**
 * Created by novy on 02.11.15.
 */
class Seller(auctionFactory: () => ActorRef) extends Actor {

  var pendingAuctions: List[AuctionRef] = List()

  override def receive: Receive = {
    case CreateAuction(timers, params, title) => register(title, timers, params, sender())
    case Registered(auction) => markAsPending(auction)
    case Unregistered(auction) => removeFromPending(auction)
    case AuctionWonBy(_, _) => unregister(sender())
    case NoOffers => unregister(sender())
  }

  private def register(title: String, timers: AuctionTimers, params: AuctionParams, sender: ActorRef): Unit = {
    val actorRefForNewAuction = auctionFactory()
    val newAuction = AuctionRef(title, actorRefForNewAuction)

    newAuction.auction ! StartAuction(timers, params)
    auctionSearch ! Register(newAuction)

    sender ! AuctionCreatedAndRegistered(newAuction)
  }

  private def markAsPending(auction: AuctionRef): Unit = pendingAuctions = auction :: pendingAuctions

  private def removeFromPending(auction: AuctionRef): Unit = pendingAuctions = pendingAuctions.filterNot(_.auction == auction)

  private def unregister(auctionActorRef: ActorRef): Unit = byAuctionRef(auctionActorRef) foreach unregister

  private def byAuctionRef(actorRef: ActorRef): Option[AuctionRef] = pendingAuctions.find(_.auction == actorRef)

  private def unregister(auction: AuctionRef): Unit = auctionSearch ! Unregister(auction)

  private def auctionSearch = context.actorSelection("/user/auction-search")
}

case object Seller {

  case class AuctionRef(title: String, auction: ActorRef)

  case class Register(auctionRef: AuctionRef)

  case class Unregister(auctionRef: AuctionRef)

  case class CreateAuction(timers: AuctionTimers, params: AuctionParams, title: String)

  case class AuctionCreatedAndRegistered(auctionRef: AuctionRef)

  def props(auctionFactory: () => ActorRef): Props = Props(new Seller(auctionFactory))
}



