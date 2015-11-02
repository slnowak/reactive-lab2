package auction.system

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import auction.system.AuctionSearch.QueryResult
import auction.system.Bidding.{BidTooLow, BidTopBySomeoneElse}
import auction.system.Buyer.{Bid, FindAuctions, StartBidding}
import auction.system.Seller.AuctionRef
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

/**
 * Created by novy on 02.11.15.
 */
class BuyerTest extends TestKit(ActorSystem("auction-system")) with WordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll with ImplicitSender {

  var objectUnderTest: ActorRef = _
  var moneyToSpend: BigDecimal = BigDecimal(666)
  var keyword: String = "scala"

  var auctionSearch: TestProbe = _

  override protected def beforeEach(): Unit = {
    auctionSearch = TestProbe()
    objectUnderTest = system.actorOf(Buyer.props(auctionSearch.ref, moneyToSpend, keyword))
  }

  override protected def afterAll(): Unit = system.shutdown()

  "Buyer" must {
    "ask for auction list when needed" in {
      // when
      objectUnderTest ! StartBidding(BigDecimal(1))

      // then
      auctionSearch.expectMsg(FindAuctions(keyword))
    }

    "should start bidding with initial price as soon as can afford" in {
      // given
      val firstAuction = TestProbe()
      val secondAuction = TestProbe()
      val thirdAuction = TestProbe()

      val initialOffer = BigDecimal(300)
      objectUnderTest ! StartBidding(initialOffer)

      // when
      val matchingAuctions = Set(
        AuctionRef("programming in scala", firstAuction.ref),
        AuctionRef("scala for impatient", secondAuction.ref),
        AuctionRef("scala in action", thirdAuction.ref)
      )

      objectUnderTest ! QueryResult(keyword, matchingAuctions)

      // then
      firstAuction.expectMsg(Bid(initialOffer))
      secondAuction.expectMsg(Bid(initialOffer))
      thirdAuction.expectNoMsg()
    }

    "should automatically top a bid if previous bid too low" in {
      // given
      val auctionProbe = TestProbe()
      objectUnderTest ! StartBidding(BigDecimal(1))

      // when
      auctionProbe.send(objectUnderTest, BidTooLow(currentAmount = BigDecimal(1), requiredAmount = BigDecimal(666)))

      // then
      auctionProbe.expectMsg(Bid(BigDecimal(666)))
    }

    "should do nothing if previous bid too low and can't afford required offer" in {
      // given
      val auctionProbe = TestProbe()
      objectUnderTest ! StartBidding(BigDecimal(1))

      // when
      auctionProbe.send(objectUnderTest, BidTooLow(currentAmount = BigDecimal(1), requiredAmount = BigDecimal(667)))

      // then
      auctionProbe.expectNoMsg()
    }

    "should automatically top a bid if someone gave higher offer" in {
      // given
      val auctionProbe = TestProbe()
      objectUnderTest ! StartBidding(BigDecimal(1))

      // when
      auctionProbe.send(
        objectUnderTest,
        BidTopBySomeoneElse(previousOffer = BigDecimal(1), currentHighestOffer = BigDecimal(555), requiredStep = BigDecimal(1))
      )

      // then
      auctionProbe.expectMsg(Bid(BigDecimal(556)))
    }

    "should do nothing if someone gave higher offer and don't have enough money to top it" in {
      // given
      val auctionProbe = TestProbe()
      objectUnderTest ! StartBidding(BigDecimal(1))

      // when
      auctionProbe.send(
        objectUnderTest,
        BidTopBySomeoneElse(previousOffer = BigDecimal(1), currentHighestOffer = BigDecimal(666), requiredStep = BigDecimal(1))
      )

      // then
      auctionProbe.expectNoMsg()
    }
  }
}
