package auction.system

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import auction.system.AuctionCoordinator.Start
import auction.system.AuctionCreated.StartAuction
import auction.system.Buyer.StartBidding
import auction.system.Data.{AuctionParams, AuctionTimers}
import auction.system.Timers.{BidTimer, DeleteTimer}

import scala.concurrent.duration._


/**
 * Created by novy on 18.10.15.
 */
object AuctionSystem extends App {
  private val system: ActorSystem = ActorSystem("auction-system")
  private val coordinator: ActorRef = system.actorOf(Props[AuctionCoordinator])
  coordinator ! Start

  system.awaitTermination()
}

class AuctionCoordinator extends Actor {

  override def receive: Receive = LoggingReceive {
    case Start => startSystem()
  }

  private def startSystem(): Unit = {
    val auction1: ActorRef = context.actorOf(Props[Auction], "auction1")
    val auction2: ActorRef = context.actorOf(Props[Auction], "auction2")

    val auction1Timers = AuctionTimers(BidTimer(30 seconds), DeleteTimer(60 seconds))
    val auction1Params = AuctionParams(step = BigDecimal(0.50), initialPrice = BigDecimal(10))

    val auction2Timers = AuctionTimers(BidTimer(30 seconds), DeleteTimer(60 seconds))
    val auction2Params = AuctionParams(step = BigDecimal(1), initialPrice = BigDecimal(0))

    auction1 ! StartAuction(auction1Timers, auction1Params)
    auction2 ! StartAuction(auction2Timers, auction2Params)

//    val buyer1: ActorRef = context.actorOf(Buyer.props(moneyToSpend = BigDecimal(20)), "buyer1")
//    val buyer2: ActorRef = context.actorOf(Buyer.props(moneyToSpend = BigDecimal(20)), "buyer2")
//    val buyer3: ActorRef = context.actorOf(Buyer.props(moneyToSpend = BigDecimal(40)), "buyer3")
//
//    buyer1 ! StartBidding(amount = BigDecimal(0), auction = auction1)
//    buyer1 ! StartBidding(amount = BigDecimal(1), auction = auction2)
//
//    buyer2 ! StartBidding(amount = BigDecimal(1), auction = auction1)
//    buyer2 ! StartBidding(amount = BigDecimal(0), auction = auction2)
//
//    buyer3 ! StartBidding(amount = BigDecimal(5), auction = auction1)
//    buyer3 ! StartBidding(amount = BigDecimal(1), auction = auction2)
  }
}

case object AuctionCoordinator {

  case object Start

}