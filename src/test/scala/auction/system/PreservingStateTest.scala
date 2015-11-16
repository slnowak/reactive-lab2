package auction.system

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import auction.system.AuctionCreatedMoveMe.StartAuction
import auction.system.Bidding.{BidAccepted, BidTooLow}
import auction.system.Buyer.Bid
import auction.system.Data.{AuctionParams, AuctionTimers, BidTimer, DeleteTimer}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by novy on 16.11.15.
  */
class PreservingStateTest extends TestKit(ActorSystem("auction-system")) with WordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll with ImplicitSender {

  var auctionId: String = _
  var auctionTimers: AuctionTimers = _
  var auctionParams: AuctionParams = _

  override protected def beforeEach(): Unit = {
    auctionId = UUID.randomUUID().toString
    auctionTimers = AuctionTimers(BidTimer(5 seconds), DeleteTimer(10 seconds))
    auctionParams = AuctionParams(step = BigDecimal(0.5), initialPrice = BigDecimal(10))
    super.beforeEach()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  "An auction must" must {

    "preserve it's internal state in case of failure" in {
      // given
      val auctionBeforeRestore = system.actorOf(Auction.props(auctionId))
      auctionBeforeRestore ! StartAuction(auctionTimers, auctionParams)

      val previousOffer = BigDecimal(666f)
      auctionBeforeRestore ! Bid(previousOffer)
      expectMsg(BidAccepted(previousOffer))

      // when
      val restoredAction = system.actorOf(Auction.props(auctionId)) // https://groups.google.com/d/msg/akka-user/RZrLVIKPPEg/1QivTZYDhgEJ

      restoredAction ! Bid(previousOffer)

      // then
      expectMsg(BidTooLow(previousOffer, previousOffer + auctionParams.step))
    }
  }

}
