package auction.system

import akka.persistence.fsm.PersistentFSM.FSMState

/**
 * Created by novy on 09.11.15.
 */
object AuctionStates {

  sealed trait AuctionState extends FSMState

  case object Idle extends AuctionState {
    override def identifier: String = "idle"
  }

  case object Created extends AuctionState {
    override def identifier: String = "created"
  }

  case object Ignored extends AuctionState {
    override def identifier: String = "ignored"
  }

  case object Activated extends AuctionState {
    override def identifier: String = "activated"
  }

  case object Sold extends AuctionState {
    override def identifier: String = "sold"
  }

}
