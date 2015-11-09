package auction.system

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActors, TestKit, TestProbe}
import auction.system.AuctionCreatedMoveMe.StartAuction
import auction.system.AuctionSearch.Registered
import auction.system.Bidding.{AuctionWonBy, NoOffers}
import auction.system.Data.{AuctionParams, AuctionTimers, BidTimer, DeleteTimer}
import auction.system.Seller._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.duration._


/**
 * Created by novy on 02.11.15.
 */
class SellerTest extends TestKit(ActorSystem()) with WordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll {

  var customActorSystem: ActorSystem = _
  var testProbe: TestProbe = _

  var objectUnderTest: ActorRef = _
  var auctionSearch: TestProbe = _
  var auctionProbe: TestProbe = _
  var auctionTimers: AuctionTimers = _
  var auctionParams: AuctionParams = _
  var auctionTitle: String = _

  override protected def beforeEach(): Unit = {
    customActorSystem = ActorSystem("auction-system")
    testProbe = new TestProbe(customActorSystem)

    auctionSearch = new TestProbe(customActorSystem)
    customActorSystem.actorOf(TestActors.forwardActorProps(auctionSearch.ref), "auction-search")

    auctionProbe = new TestProbe(customActorSystem)
    objectUnderTest = customActorSystem.actorOf(Seller.props(() => auctionProbe.ref))

    auctionTimers = AuctionTimers(BidTimer(10 seconds), DeleteTimer(5 seconds))
    auctionParams = AuctionParams(BigDecimal(0.5), BigDecimal(10))
    auctionTitle = "auction"
  }

  override protected def afterEach(): Unit = customActorSystem.terminate()

  override protected def afterAll(): Unit = system.terminate()

  "Seller" must {

    "respond to create auction request" in {
      // when
      testProbe.send(objectUnderTest, CreateAuction(auctionTimers, auctionParams, auctionTitle))

      // then
      testProbe.expectMsg(AuctionCreatedAndRegistered(AuctionRef(auctionTitle, auctionProbe.ref)))
    }

    "start created auction" in {
      // when
      testProbe.send(objectUnderTest, CreateAuction(auctionTimers, auctionParams, auctionTitle))

      // then
      auctionProbe.expectMsg(StartAuction(auctionTimers, auctionParams))
    }

    "register new auction in AuctionSearch on request" in {
      // when
      testProbe.send(objectUnderTest, CreateAuction(auctionTimers, auctionParams, auctionTitle))

      // then
      auctionSearch.expectMsg(Register(AuctionRef(auctionTitle, auctionProbe.ref)))
    }

    "unregister existing auction from AuctionSearch if it ends without any offer" in {
      // given
      testProbe.send(objectUnderTest, CreateAuction(auctionTimers, auctionParams, auctionTitle))
      auctionSearch.send(objectUnderTest, Registered(AuctionRef(auctionTitle, auctionProbe.ref)))

      // when
      auctionProbe.send(objectUnderTest, NoOffers)

      //then
      auctionSearch.expectMsg(Register(AuctionRef(auctionTitle, auctionProbe.ref)))
      auctionSearch.expectMsg(Unregister(AuctionRef(auctionTitle, auctionProbe.ref)))
    }

    "unregister existing auction from AuctionSearch if it ends with a winner" in {
      // given
      testProbe.send(objectUnderTest, CreateAuction(auctionTimers, auctionParams, auctionTitle))
      auctionSearch.send(objectUnderTest, Registered(AuctionRef(auctionTitle, auctionProbe.ref)))

      // when
      val winner = new TestProbe(customActorSystem)
      objectUnderTest.tell(AuctionWonBy(winner.ref, BigDecimal(666)), auctionProbe.ref)

      //then
      auctionSearch.expectMsg(Register(AuctionRef(auctionTitle, auctionProbe.ref)))
      auctionSearch.expectMsg(Unregister(AuctionRef(auctionTitle, auctionProbe.ref)))
    }
  }
}
