package auction.system

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.event.LoggingReceive
import auction.system.AuctionCoordinator.Start
import auction.system.Buyer.StartBidding

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
    val auction1: ActorRef = context.actorOf(Auction.props(step = BigDecimal(0.50), initialPrice = BigDecimal(12)), "auction1")
    val auction2: ActorRef = context.actorOf(Auction.props(step = BigDecimal(1), initialPrice = BigDecimal(0)), "auction2")

    val buyer1: ActorRef = context.actorOf(Buyer.props(moneyToSpend = BigDecimal(10)), "buyer1")
    val buyer2: ActorRef = context.actorOf(Buyer.props(moneyToSpend = BigDecimal(20)), "buyer2")
    val buyer3: ActorRef = context.actorOf(Buyer.props(moneyToSpend = BigDecimal(40)), "buyer3")

    buyer1 ! StartBidding(amount = BigDecimal(0), auction = auction1)
    buyer1 ! StartBidding(amount = BigDecimal(1), auction = auction2)

    buyer2 ! StartBidding(amount = BigDecimal(1), auction = auction1)
    buyer2 ! StartBidding(amount = BigDecimal(0), auction = auction2)

    buyer3 ! StartBidding(amount = BigDecimal(5), auction = auction1)
    buyer3 ! StartBidding(amount = BigDecimal(1), auction = auction2)
  }
}

case object AuctionCoordinator {

  case object Start

}