package auction.system.notifications

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, SupervisorStrategy}

/**
  * Created by novy on 23.11.15.
  */
class Notifier(auctionPublisher: ActorRef) extends Actor {

  override def receive: Receive = {
    case _ =>
  }

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = false) {
    case _ => Restart
  }
}

object Notifier {

  trait NotificationPayload

  case class Notification(target: ActorRef, payload: NotificationPayload)

}

