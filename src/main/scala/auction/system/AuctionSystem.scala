package auction.system

import akka.actor._
import akka.event.LoggingReceive
import auction.system.AuctionCoordinator.Start
import auction.system.Buyer.StartBidding
import auction.system.Data.{AuctionParams, AuctionTimers, BidTimer, DeleteTimer}
import auction.system.Seller.CreateAuction
import auction.system.notifications.{AuctionPublisher, Notifier}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._


/**
  * Created by novy on 18.10.15.
  */
object AuctionSystem extends App {
  val config: Config = ConfigFactory.load()

  //  val auctionPublisherSystem: ActorSystem = ActorSystem("auction-publisher", config.getConfig("auction-publisher").withFallback(config))
  //  auctionPublisherSystem.actorOf(Props[AuctionPublisher], "auction-publisher")

  val auctionSystem: ActorSystem = ActorSystem("auction-system", config.getConfig("auction-system").withFallback(config))
  val coordinator: ActorRef = auctionSystem.actorOf(Props[AuctionCoordinator])
  val auctionSearch: ActorRef = auctionSystem.actorOf(Props[AuctionSearch], "auction-search")
  coordinator ! Start(auctionSearch)
}

object RemoteAuctionPublisher extends App {
  val config: Config = ConfigFactory.load()

  val auctionPublisherSystem: ActorSystem = ActorSystem("auction-publisher", config.getConfig("auction-publisher").withFallback(config))
  auctionPublisherSystem.actorOf(Props[AuctionPublisher], "auction-publisher")
}

class AuctionCoordinator extends Actor {

  override def receive: Receive = LoggingReceive {
    case Start(auctionSearch) => startSystem(auctionSearch)
  }

  private def startSystem(auctionSearch: ActorRef): Unit = {
    val auction1Timers = AuctionTimers(BidTimer(60 seconds), DeleteTimer(120 seconds))
    val auction1Params = AuctionParams(title = "scala for impatient", step = BigDecimal(0.50), initialPrice = BigDecimal(10))

    val auction2Timers = AuctionTimers(BidTimer(60 seconds), DeleteTimer(120 seconds))
    val auction2Params = AuctionParams(title = "scala for java programmers", step = BigDecimal(1), initialPrice = BigDecimal(0))

    val notifier: ActorRef = context.actorOf(Notifier.props(remotePublisher))
    val auctionFactory = () => context.actorOf(Auction.props(notifier = notifier))

    val seller = context.actorOf(Seller.props(auctionFactory))

    seller ! CreateAuction(auction1Timers, auction1Params)
    seller ! CreateAuction(auction2Timers, auction2Params)

    val buyer1: ActorRef = context.actorOf(Buyer.props(auctionSearch, BigDecimal(10), "java"), "buyer1")
    val buyer2: ActorRef = context.actorOf(Buyer.props(auctionSearch, BigDecimal(10), "java"), "buyer2")

    Thread.sleep(1000)

    buyer1 ! StartBidding(BigDecimal(0.5))
    buyer2 ! StartBidding(BigDecimal(2))
  }

  private def remotePublisher(): ActorSelection = context.actorSelection("akka.tcp://auction-publisher@127.0.0.1:2553/user/auction-publisher")

}

case object AuctionCoordinator {

  case class Start(auctionSearch: ActorRef)

}