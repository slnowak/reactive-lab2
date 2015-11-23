package auction.system.notifications

import akka.actor._
import auction.system.notifications.Notifier.NotificationPayload

/**
  * Created by novy on 23.11.15.
  */
class Notifier(auctionPublisher: () => ActorSelection) extends Actor {

  override def receive: Receive = {
    case payload: NotificationPayload => sendNotification(payload)
  }

  private def sendNotification(payload: NotificationPayload): Unit = {
    auctionPublisher() ! payload
  }

  //  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = false) {
  //    case _ => Restart
  //  }
}

object Notifier {

  def props(externalPublisher: () => ActorSelection): Props = Props(new Notifier(externalPublisher))

  trait NotificationPayload

  case class Notification(target: ActorSelection, payload: NotificationPayload)

}

