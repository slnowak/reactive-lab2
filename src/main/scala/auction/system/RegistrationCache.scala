package auction.system

import auction.system.Seller.AuctionRef

/**
  * Created by novy on 07.12.15.
  */

case class RegistrationCache(expectedAcks: Int) {

  val registerAcks: AcknowledgmentCache = AcknowledgmentCache()
  val unregisterAcks: AcknowledgmentCache = AcknowledgmentCache()

  def scheduleCallbackOnRegisterActionAcknowledged(auction: AuctionRef,
                                                   onExpectedAcksAcquired: AuctionRef => Unit): Unit = {

    registerAcks.scheduleCallbackOnAcknowledged(auction, expectedAcks, onExpectedAcksAcquired)
  }

  def registerActionAcknowledged(auction: AuctionRef): Unit = registerAcks.acknowledge(auction)

  def scheduleCallbackOnUnregisterActionAcknowledged(auction: AuctionRef,
                                                     onExpectedAcksAcquired: AuctionRef => Unit): Unit = {

    unregisterAcks.scheduleCallbackOnAcknowledged(auction, expectedAcks, onExpectedAcksAcquired)
  }

  def unregisterActionAcknowledged(auction: AuctionRef): Unit = unregisterAcks.acknowledge(auction)
}

case class AcknowledgmentCache() {

  var acks: Map[AuctionRef, AcknowledgmentItem] = Map()

  def scheduleCallbackOnAcknowledged(auction: AuctionRef,
                                     expectedAcks: Int,
                                     onExpectedAcksAcquired: AuctionRef => Unit): Unit = {

    acks += (auction -> AcknowledgmentItem(expectedAcks, 0, onExpectedAcksAcquired))
  }

  def acknowledge(auction: AuctionRef): Unit = {
    val maybeItem: Option[AcknowledgmentItem] = acks.get(auction)
    maybeItem foreach increaseAckCountOrPerformAcknowledgmentCallback(auction)
  }

  def increaseAckCountOrPerformAcknowledgmentCallback(auctionRef: AuctionRef)(item: AcknowledgmentItem): Unit = {
    if (item.actualAcks + 1 == item.expectedAcks) {
      acks -= auctionRef
      item.onExpectedAcksAcquired(auctionRef)
    } else {
      acks += (auctionRef -> AcknowledgmentItem(item.expectedAcks, item.actualAcks + 1, item.onExpectedAcksAcquired))
    }
  }
}

case class AcknowledgmentItem(expectedAcks: Int, actualAcks: Int, onExpectedAcksAcquired: AuctionRef => Unit)