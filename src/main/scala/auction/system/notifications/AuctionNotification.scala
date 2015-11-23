package auction.system.notifications

import auction.system.Data.Bid
import auction.system.notifications.Notifier.NotificationPayload

/**
  * Created by novy on 23.11.15.
  */
sealed trait AuctionNotification extends NotificationPayload {
  def auctionTitle: String
}

case class NewOfferArrived(auctionTitle: String, newHighestOffer: Bid) extends AuctionNotification

case class EndedWithoutOffers(auctionTitle: String) extends AuctionNotification

case class EndedWithWinner(auctionTitle: String, winningOffer: Bid) extends AuctionNotification
