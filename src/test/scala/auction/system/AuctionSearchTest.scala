package auction.system

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import auction.system.auctionsearch.AuctionSearch
import AuctionSearch.{QueryResult, Registered, Unregistered}
import auction.system.Buyer.FindAuctions
import auction.system.Seller.{AuctionRef, Register, Unregister}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

/**
 * Created by novy on 02.11.15.
 */
class AuctionSearchTest extends TestKit(ActorSystem("auction-system")) with WordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll with ImplicitSender {

  var objectUnderTest: ActorRef = _

  override protected def beforeEach(): Unit = {
    objectUnderTest = system.actorOf(Props[AuctionSearch])
  }

  override protected def afterAll(): Unit = system.terminate()

  "An auction search" must {

    "respond to register request" in {
      // given
      val auction = AuctionRef("an auction", Actor.noSender)

      // when
      objectUnderTest ! Register(auction)

      // then
      expectMsg(Registered(auction))
    }

    "respond to unregister request" in {
      // given
      val auction = AuctionRef("an auction", Actor.noSender)
      objectUnderTest ! Register(auction)

      // when
      objectUnderTest ! Unregister(auction)

      // then
      expectMsg(Registered(auction))
      expectMsg(Unregistered(auction))
    }

    "return matching auctions on query" in {
      // given
      val matchingAuction = AuctionRef("young teen doing anal", null)
      val anotherMatching = AuctionRef("anal adventures", null)
      val completelyDifferent = AuctionRef("Scala in Action", null)

      objectUnderTest.tell(Register(matchingAuction), Actor.noSender)
      objectUnderTest.tell(Register(anotherMatching), Actor.noSender)
      objectUnderTest.tell(Register(completelyDifferent), Actor.noSender)

      // when
      objectUnderTest ! FindAuctions("anal")

      // then
      expectMsg(QueryResult("anal", Set(matchingAuction, anotherMatching)))
    }

    "return QueryResult with empty set if there's no match" in {
      // when
      objectUnderTest ! FindAuctions("foo")

      // then
      expectMsg(QueryResult("foo", Set()))
    }
  }

  "unregistered actions should not be queried" in {
    // given
    val matchingAuction = AuctionRef("young teen doing anal", null)
    val unregisteredMatching = AuctionRef("anal adventures", null)
    val completelyDifferent = AuctionRef("Scala in Action", null)

    objectUnderTest.tell(Register(matchingAuction), Actor.noSender)
    objectUnderTest.tell(Register(unregisteredMatching), Actor.noSender)
    objectUnderTest.tell(Register(completelyDifferent), Actor.noSender)
    objectUnderTest.tell(Unregister(unregisteredMatching), Actor.noSender)

    // when
    objectUnderTest ! FindAuctions("anal")

    // then
    expectMsg(QueryResult("anal", Set(matchingAuction)))
  }
}
