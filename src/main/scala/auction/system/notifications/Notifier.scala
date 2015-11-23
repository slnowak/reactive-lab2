package auction.system.notifications

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor._
import auction.system.notifications.Notifier.{Notification, NotificationPayload}
import auction.system.notifications.NotifierRequest.SuccessfullyDeliveredNotification

/**
  * Created by novy on 23.11.15.
  */
class Notifier(auctionPublisher: () => ActorSelection) extends Actor with ActorLogging {

  private val maxRetries = 10000

  override def receive: Receive = {
    case payload: NotificationPayload => sendNotification(payload)
    case SuccessfullyDeliveredNotification(payload) => println(s"Successfully delivered notification $payload")
  }

  private def sendNotification(payload: NotificationPayload): Unit = {
    context.actorOf(NotifierRequest.props(notificationToSend = Notification(auctionPublisher(), payload)))
  }

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = maxRetries, loggingEnabled = false) {
    case _: ActorNotFound =>
      log.debug("Failed to found auctionPublisher, retrying")
      Restart

    case _ => Escalate
  }
}

object Notifier {

  def props(externalPublisher: () => ActorSelection): Props = Props(new Notifier(externalPublisher))

  trait NotificationPayload

  case class Notification(target: ActorSelection, payload: NotificationPayload)

}

