package auction.system

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit, TestProbe}
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
    auctionTimers = AuctionTimers(BidTimer(50 seconds), DeleteTimer(100 seconds))
    auctionParams = AuctionParams(title = "title", step = BigDecimal(0.5), initialPrice = BigDecimal(10))
    super.beforeEach()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  "An auction must" must {

    "preserve it's internal state in case of failure" in {
      // given
      val dummyNotifier: () => ActorRef = () => TestProbe().ref

      val auctionBeforeRestore = system.actorOf(Auction.props(dummyNotifier, auctionId))
      auctionBeforeRestore ! StartAuction(auctionTimers, auctionParams)

      val previousOffer = BigDecimal(666f)
      auctionBeforeRestore ! Bid(previousOffer)
      TimeUnit.SECONDS.sleep(1)
      expectMsg(BidAccepted(previousOffer))

      // when
      val restoredAction = system.actorOf(Auction.props(dummyNotifier, auctionId)) // https://groups.google.com/d/msg/akka-user/RZrLVIKPPEg/1QivTZYDhgEJ

      restoredAction ! Bid(previousOffer)

      // then
      expectMsg(BidTooLow(previousOffer, previousOffer + auctionParams.step))
    }
  }
}
