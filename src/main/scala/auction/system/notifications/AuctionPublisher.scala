package auction.system.notifications

import akka.actor.Actor
import auction.system.Data.Bid
import auction.system.notifications.AuctionPublisher.ReceivedNotification

/**
  * Created by novy on 23.11.15.
  */
class AuctionPublisher extends Actor {

  override def receive: Receive = {
    case NewOfferArrived(title, Bid(newOffer, buyer)) =>
      println(s"New offer for auction $title: $newOffer by $buyer")
      sender() ! ReceivedNotification

    case EndedWithoutOffers(title) =>
      println(s"There were no offers for auction $title")
      sender() ! ReceivedNotification

    case EndedWithWinner(title, Bid(winningPrice, winner)) =>
      println(s"Buyer $winner won auction $title with price $winningPrice")
      sender() ! ReceivedNotification
  }
}

object AuctionPublisher {

  case object ReceivedNotification

}
