package auction.system

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import auction.system.AuctionSearch.Registered
import auction.system.Bidding.{AuctionWonBy, NoOffers}
import auction.system.Data.{AuctionParams, AuctionTimers}
import auction.system.Seller._
import auction.system.Timers.{BidTimer, DeleteTimer}
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
  var auction: ActorRef = _
  var auctionTimers: AuctionTimers = _
  var auctionParams: AuctionParams = _
  var auctionTitle: String = _

  override protected def beforeEach(): Unit = {
    customActorSystem = ActorSystem("auction-system")
    testProbe = new TestProbe(customActorSystem)

    auctionSearch = new TestProbe(customActorSystem)
    customActorSystem.actorOf(forwardingActorProps(auctionSearch), "auction-search")

    auction = customActorSystem.actorOf(Props[Auction])
    objectUnderTest = customActorSystem.actorOf(Seller.props(() => auction))

    auctionTimers = AuctionTimers(BidTimer(10 seconds), DeleteTimer(5 seconds))
    auctionParams = AuctionParams(BigDecimal(0.5), BigDecimal(10))
    auctionTitle = "auction"
  }

  override protected def afterEach(): Unit = customActorSystem.shutdown()

  override protected def afterAll(): Unit = system.shutdown()

  "Seller" must {

    "respond to create auction request" in {
      // when
      objectUnderTest.tell(CreateAuction(auctionTimers, auctionParams, auctionTitle), testProbe.ref)

      // then
      testProbe.expectMsg(AuctionCreatedAndRegistered(AuctionRef(auctionTitle, auction)))
    }

    "register new auction in AuctionSearch on request" in new TestKit(system) {
      // when
      objectUnderTest.tell(CreateAuction(auctionTimers, auctionParams, auctionTitle), testProbe.ref)

      // then
      auctionSearch.expectMsg(Register(AuctionRef(auctionTitle, auction)))
    }

    "unregister existing auction from AuctionSearch if it ends without any offer" in {
      // given
      objectUnderTest.tell(CreateAuction(auctionTimers, auctionParams, auctionTitle), testProbe.ref)
      objectUnderTest.tell(Registered(AuctionRef(auctionTitle, auction)), auctionSearch.ref)

      // when
      objectUnderTest.tell(NoOffers, auction)

      //then
      auctionSearch.expectMsg(Register(AuctionRef(auctionTitle, auction)))
      auctionSearch.expectMsg(Unregister(AuctionRef(auctionTitle, auction)))
    }

    "unregister existing auction from AuctionSearch if it ends with a winner" in {
      // given
      objectUnderTest.tell(CreateAuction(auctionTimers, auctionParams, auctionTitle), testProbe.ref)
      objectUnderTest.tell(Registered(AuctionRef(auctionTitle, auction)), auctionSearch.ref)

      // when
      val winner = new TestProbe(customActorSystem)
      objectUnderTest.tell(AuctionWonBy(winner.ref, BigDecimal(666)), auction)

      //then
      auctionSearch.expectMsg(Register(AuctionRef(auctionTitle, auction)))
      auctionSearch.expectMsg(Unregister(AuctionRef(auctionTitle, auction)))
    }
  }

  private def forwardingActorProps(testProbe: TestProbe): Props = {
    Props(new Actor {
      override def receive: Receive = {
        case x => testProbe.ref forward x
      }
    })
  }
}
