package auction.system

import akka.actor.{Actor, ActorRef}

/**
 * Created by novy on 02.11.15.
 */
class Seller extends Actor {
  override def receive: Receive = ???
}

case object Seller {

  case class AuctionRef(title: String, auction: ActorRef)

  case class Register(auctionRef: AuctionRef)

  case class Unregister(auctionRef: AuctionRef)

}



