package auction.system

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import auction.system.AuctionCreated.{BidTimerExpired, StartAuction}
import auction.system.Bidding._
import auction.system.Buyer.Bid
import auction.system.Data.{AuctionParams, AuctionTimers}
import auction.system.Timers.{BidTimer, DeleteTimer}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by novy on 02.11.15.
 */
class AuctionSpec extends TestKit(ActorSystem("auction-system")) with WordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll with ImplicitSender {

  var objectUnderTest: ActorRef = _
  var seller: TestProbe = _
  var buyer: TestProbe = _
  var auctionTimers: AuctionTimers = _
  var auctionParams: AuctionParams = _

  override protected def beforeEach(): Unit = {
    objectUnderTest = system.actorOf(Props[Auction])
    seller = TestProbe()
    buyer = TestProbe()
    auctionTimers = AuctionTimers(BidTimer(5 seconds), DeleteTimer(10 seconds))
    auctionParams = AuctionParams(step = BigDecimal(0.5), initialPrice = BigDecimal(10))
  }

  override protected def afterAll(): Unit = system.shutdown()

  "An auction" must {

    "notify buyer the bid was accepted if it exceeds initial price" in {
      // given:
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      // when
      val newOffer: BigDecimal = BigDecimal(10)
      objectUnderTest.tell(Bid(newOffer), buyer.ref)

      // then
      buyer.expectMsg(BidAccepted(newOffer))
    }

    "notify buyer the bid was too low and provide with required price" in {
      // given:
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      // when
      objectUnderTest.tell(Bid(BigDecimal(9.99)), buyer.ref)

      // then
      buyer.expectMsg(BidTooLow(currentAmount = BigDecimal(9.99), requiredAmount = BigDecimal(10)))
    }

    "notify buyer his offer was accepted if exceeds previous offer" in {
      // given
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      val anotherSender = TestProbe()
      val previousHighestOffer = BigDecimal(10)
      objectUnderTest.tell(msg = Bid(previousHighestOffer), sender = anotherSender.ref)

      // when
      val newOffer = previousHighestOffer + auctionParams.step
      objectUnderTest.tell(Bid(newOffer), buyer.ref)

      // then
      buyer.expectMsg(BidAccepted(newOffer))
    }

    "notify buyer his offer was too low and provide with lowest possible amount" in {
      // given
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      val anotherBuyer = TestProbe()
      val previousHighestOffer = BigDecimal(10)
      objectUnderTest.tell(msg = Bid(previousHighestOffer), sender = anotherBuyer.ref)

      // when
      val tooLowOffer = previousHighestOffer
      objectUnderTest.tell(Bid(tooLowOffer), buyer.ref)

      // then
      buyer.expectMsg(BidTooLow(
        currentAmount = tooLowOffer,
        requiredAmount = previousHighestOffer + auctionParams.step
      ))
    }

    "notify buyer if his bid was top by someone else" in {
      // given
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)
      val previousOffer = BigDecimal(10)
      objectUnderTest.tell(Bid(previousOffer), buyer.ref)

      // when
      val offeredBySomeoneElse = BigDecimal(666)
      val someoneElse = TestProbe()
      objectUnderTest.tell(Bid(offeredBySomeoneElse), someoneElse.ref)

      // then
      buyer.expectMsg(BidAccepted(previousOffer))
      buyer.expectMsg(BidTopBySomeoneElse(previousOffer, offeredBySomeoneElse, auctionParams.step))
    }

    "notify winner when auction ends" in {
      // given
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      val anotherBuyer = TestProbe()
      objectUnderTest.tell(Bid(BigDecimal(150)), buyer.ref)
      objectUnderTest.tell(Bid(BigDecimal(151)), anotherBuyer.ref)
      objectUnderTest.tell(Bid(BigDecimal(666)), buyer.ref)

      // when
      objectUnderTest ! BidTimerExpired

      // then
      buyer.expectMsg(BidAccepted(BigDecimal(150)))
      buyer.expectMsg(BidTopBySomeoneElse(BigDecimal(150), BigDecimal(151), auctionParams.step))
      buyer.expectMsg(BidAccepted(BigDecimal(666)))

      buyer.expectMsg(AuctionWon(BigDecimal(666)))
    }

    "notify seller about winner" in {
      // given
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      val winningOffer = BigDecimal(666)
      objectUnderTest.tell(Bid(winningOffer), buyer.ref)

      // when
      objectUnderTest ! BidTimerExpired

      // then
      seller.expectMsg(AuctionWonBy(buyer.ref, winningOffer))
    }

    "notify seller there were no offers" in {
      // given
      objectUnderTest.tell(StartAuction(auctionTimers, auctionParams), seller.ref)

      // when
      objectUnderTest ! BidTimerExpired

      // then
      seller.expectMsg(NoOffers)
    }
  }
}
