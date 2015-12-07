package auction.system

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.routing.RoundRobinRoutingLogic
import akka.testkit._
import auction.system.auctionsearch.{MasterSearch, AuctionSearch}
import AuctionSearch.{Registered, Unregistered}
import auction.system.Seller.{AuctionRef, Register, Unregister}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

import scala.collection.mutable

/**
  * Created by novy on 07.12.15.
  */
class MasterSearchTest extends TestKit(ActorSystem("ww")) with WordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll with ImplicitSender {

  var objectUnderTest: TestActorRef[MasterSearch] = _
  var firstAuctionSearchMock: TestProbe = _
  var secondAuctionSearchMock: TestProbe = _

  override protected def beforeEach(): Unit = {
    firstAuctionSearchMock = TestProbe()
    secondAuctionSearchMock = TestProbe()

    val routees: mutable.Queue[TestProbe] = mutable.Queue(firstAuctionSearchMock, secondAuctionSearchMock)
    def routeeFactory: () => ActorRef = () => routees.dequeue().ref

    objectUnderTest = TestActorRef(MasterSearch.props(2, RoundRobinRoutingLogic(), routeeFactory))
  }

  override protected def afterAll(): Unit = system.terminate()

  "Master Search" must {

    "forward all 'register' messages to routees" in {
      // given
      val registrationMessage: Register = Register(AuctionRef("auction", Actor.noSender))

      // when
      objectUnderTest ! registrationMessage

      // then
      firstAuctionSearchMock.expectMsg(registrationMessage)
      secondAuctionSearchMock.expectMsg(registrationMessage)
    }

    "forward all 'unregister' messages to routees" in {
      // given
      val unregisterMessage: Unregister = Unregister(AuctionRef("auction", Actor.noSender))

      // when
      objectUnderTest ! unregisterMessage

      // then
      firstAuctionSearchMock.expectMsg(unregisterMessage)
      secondAuctionSearchMock.expectMsg(unregisterMessage)
    }

    "not respond with 'Registered' until all routees acknowledged" in {
      // given
      val auction = AuctionRef("auction", Actor.noSender)

      // when
      objectUnderTest ! Register(auction)
      objectUnderTest ! Registered(auction)

      // then
      expectNoMsg()
    }

    "respond with 'Registered' as soon as all routees acknowledged" in {
      // given
      val auction = AuctionRef("auction", Actor.noSender)

      // when
      objectUnderTest ! Register(auction)
      objectUnderTest ! Registered(auction)
      objectUnderTest ! Registered(auction)

      // then
      expectMsg(Registered(auction))
    }

    "not respond with 'Unregistered' until all routees acknowledged" in {
      // given
      ignoreRegisteredAcks()
      val auction = AuctionRef("auction", Actor.noSender)
      objectUnderTest ! Register(auction)
      objectUnderTest ! Registered(auction)
      objectUnderTest ! Registered(auction)

      // when
      objectUnderTest ! Unregister(auction)
      objectUnderTest ! Unregistered(auction)

      // then
      expectNoMsg()
    }

    "respond with 'Unregistered' as soon as all routees acknowledged" in {
      // given
      ignoreRegisteredAcks()
      val auction = AuctionRef("auction", Actor.noSender)
      objectUnderTest ! Register(auction)
      objectUnderTest ! Registered(auction)
      objectUnderTest ! Registered(auction)

      // when
      objectUnderTest ! Unregister(auction)
      objectUnderTest ! Unregistered(auction)
      objectUnderTest ! Unregistered(auction)

      // then
      expectMsg(Unregistered(auction))
    }
  }

  private def ignoreRegisteredAcks(): Unit = {
    ignoreMsg({
      case _: Registered => true
      case _ => false
    })
  }
}
