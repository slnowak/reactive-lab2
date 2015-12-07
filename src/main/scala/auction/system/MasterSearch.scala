package auction.system

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.routing.{ActorRefRoutee, Broadcast, Router, RoutingLogic}
import auction.system.AuctionSearch.{Registered, Unregistered}
import auction.system.Buyer.FindAuctions
import auction.system.Seller.{AuctionRef, Register, Unregister}

/**
  * Created by novy on 07.12.15.
  */

class MasterSearch(numberOfRoutees: Int, dispatchingStrategy: RoutingLogic, routeeFactory: () => ActorRef) extends Actor {

  val ackCache: RegistrationCache = RegistrationCache(numberOfRoutees)

  val router = createRouterWith(routeesNumber = numberOfRoutees, routingStrategy = dispatchingStrategy)

  private def createRouterWith(routeesNumber: Int, routingStrategy: RoutingLogic): Router = {
    val routees = Vector.fill(routeesNumber)(singleRoutee)
    Router(routingStrategy, routees)
  }

  private def singleRoutee: ActorRefRoutee = {
    val routee = routeeFactory()
    ActorRefRoutee(routee)
  }

  override def receive: Receive = LoggingReceive {
    case msg@Register(auction) =>
      scheduleRegisteredAcknowledgmentFor(auction, sender())
      router.route(Broadcast(msg), self)

    case msg@Unregister(auction) =>
      scheduleUnregisteredAcknowledgmentFor(auction, sender())
      router.route(Broadcast(msg), self)

    case msg: FindAuctions =>
      router.route(msg, sender())

    case Registered(auction) =>
      ackCache.registerActionAcknowledged(auction)

    case Unregistered(auction) =>
      ackCache.unregisterActionAcknowledged(auction)
  }

  def scheduleRegisteredAcknowledgmentFor(auction: AuctionRef, seller: ActorRef) = {
    ackCache.scheduleCallbackOnRegisterActionAcknowledged(auction, auction => seller ! Registered(auction))
  }

  def scheduleUnregisteredAcknowledgmentFor(auction: AuctionRef, seller: ActorRef) = {
    ackCache.scheduleCallbackOnUnregisterActionAcknowledged(auction, auction => seller ! Unregistered(auction))
  }
}

object MasterSearch {
  def props(numberOfRoutees: Int, dispatchingStrategy: RoutingLogic, routeeFactory: () => ActorRef): Props =
    Props(new MasterSearch(numberOfRoutees, dispatchingStrategy, routeeFactory))
}
