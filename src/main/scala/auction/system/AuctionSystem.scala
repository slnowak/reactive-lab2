package auction.system

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import auction.system.AuctionCoordinator.Start
import auction.system.Buyer.StartBidding
import auction.system.Data.{AuctionParams, AuctionTimers, BidTimer, DeleteTimer}
import auction.system.Seller.CreateAuction

import scala.concurrent.duration._


/**
  * Created by novy on 18.10.15.
  */
object AuctionSystem extends App {
  private val system: ActorSystem = ActorSystem("auction-system")
  private val coordinator: ActorRef = system.actorOf(Props[AuctionCoordinator])
  private val auctionSearch: ActorRef = system.actorOf(Props[AuctionSearch], "auction-search")
  coordinator ! Start(auctionSearch)

  system.awaitTermination()
}

class AuctionCoordinator extends Actor {

  override def receive: Receive = LoggingReceive {
    case Start(auctionSearch) => startSystem(auctionSearch)
  }

  private def startSystem(auctionSearch: ActorRef): Unit = {
    val auction1Timers = AuctionTimers(BidTimer(30 seconds), DeleteTimer(60 seconds))
    val auction1Params = AuctionParams(title = "scala for impatient", step = BigDecimal(0.50), initialPrice = BigDecimal(10))

    val auction2Timers = AuctionTimers(BidTimer(30 seconds), DeleteTimer(60 seconds))
    val auction2Params = AuctionParams(title = "scala for java programmers", step = BigDecimal(1), initialPrice = BigDecimal(0))

    val auctionFactory = () => context.actorOf(Props[Auction])
    val seller = context.actorOf(Seller.props(auctionFactory))

    seller ! CreateAuction(auction1Timers, auction1Params)
    seller ! CreateAuction(auction2Timers, auction2Params)

    val buyer1: ActorRef = context.actorOf(Buyer.props(auctionSearch, BigDecimal(500), "scala"), "buyer1")
    val buyer2: ActorRef = context.actorOf(Buyer.props(auctionSearch, BigDecimal(220), "java"), "buyer2")

    Thread.sleep(1000)

    buyer1 ! StartBidding(BigDecimal(2))
    buyer2 ! StartBidding(BigDecimal(2))
  }
}

case object AuctionCoordinator {

  case class Start(auctionSearch: ActorRef)

}