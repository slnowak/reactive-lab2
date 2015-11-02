package auction.system

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import auction.system.AuctionCreated.{BidTimerExpired, StartAuction}
import auction.system.Bidding.{AuctionWon, BidAccepted, BidTooLow, BidTopBySomeoneElse}
import auction.system.Buyer.Bid
import auction.system.Data.{AuctionParams, AuctionTimers}
import auction.system.Timers.{BidTimer, DeleteTimer}
import org.scalatest.{BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by novy on 02.11.15.
 */
class AuctionSpec extends TestKit(ActorSystem("auction-system")) with WordSpecLike with BeforeAndAfterEach with ImplicitSender {

  var objectUnderTest: ActorRef = _
  var auctionTimers: AuctionTimers = _
  var auctionParams: AuctionParams = _

  override protected def beforeEach(): Unit = {
    objectUnderTest = system.actorOf(Props[Auction])
    auctionTimers = AuctionTimers(BidTimer(5 seconds), DeleteTimer(10 seconds))
    auctionParams = AuctionParams(step = BigDecimal(0.5), initialPrice = BigDecimal(10))
  }

  "An auction" must {

    // todo: initial offer - extract to separate test case
    "notify buyer the bid was accepted if it exceeds initial price" in {
      // given:
      objectUnderTest ! StartAuction(auctionTimers, auctionParams)

      // when
      val newOffer: BigDecimal = BigDecimal(10)
      objectUnderTest ! Bid(newOffer)

      // then
      expectMsg(BidAccepted(newOffer))
    }

    "notify buyer the bid was too low and provide with required price" in {
      // given:
      objectUnderTest ! StartAuction(auctionTimers, auctionParams)

      // when
      objectUnderTest ! Bid(BigDecimal(9.99))

      // then
      expectMsg(BidTooLow(currentAmount = BigDecimal(9.99), requiredAmount = BigDecimal(10)))
    }

    // todo: auction in progress - separate test case
    "notify buyer his offer was accepted if exceeds previous offer" in {
      // given
      objectUnderTest ! StartAuction(auctionTimers, auctionParams)
      val previousHighestOffer = BigDecimal(10)
      objectUnderTest.tell(msg = Bid(previousHighestOffer), sender = Actor.noSender)

      // when
      val newOffer = previousHighestOffer + auctionParams.step
      objectUnderTest ! Bid(newOffer)

      // then
      expectMsg(BidAccepted(newOffer))
    }

    "notify buyer his offer was too low and provide with lowest possible amount" in {
      // given
      objectUnderTest ! StartAuction(auctionTimers, auctionParams)
      val previousHighestOffer = BigDecimal(10)
      objectUnderTest.tell(msg = Bid(previousHighestOffer), sender = Actor.noSender)

      // when
      val tooLowOffer = previousHighestOffer
      objectUnderTest ! Bid(tooLowOffer)

      // then
      expectMsg(BidTooLow(
        currentAmount = tooLowOffer,
        requiredAmount = previousHighestOffer + auctionParams.step
      ))
    }

    "notify buyer if his bid was top by someone else" in {
      // given
      objectUnderTest ! StartAuction(auctionTimers, auctionParams)
      val previousOffer = BigDecimal(10)
      objectUnderTest ! Bid(previousOffer)

      // when
      val offeredBySomeoneElse = BigDecimal(666)
      val someoneElse = Actor.noSender
      objectUnderTest.tell(Bid(offeredBySomeoneElse), someoneElse)

      // then
      expectMsg(BidAccepted(previousOffer))
      expectMsg(BidTopBySomeoneElse(previousOffer, offeredBySomeoneElse, auctionParams.step))
    }

    // todo: anothertestcase??
    "notify winner when auction ends" in {
      // given
      objectUnderTest ! StartAuction(auctionTimers, auctionParams)

      objectUnderTest ! Bid(BigDecimal(150))
      objectUnderTest.tell(Bid(BigDecimal(151)), Actor.noSender)
      objectUnderTest ! Bid(BigDecimal(666))

      // when
      objectUnderTest ! BidTimerExpired

      // then
      expectMsg(BidAccepted(BigDecimal(150)))
      expectMsg(BidTopBySomeoneElse(BigDecimal(150), BigDecimal(151), auctionParams.step))
      expectMsg(BidAccepted(BigDecimal(666)))

      expectMsg(AuctionWon(BigDecimal(666)))
    }

  }
}
