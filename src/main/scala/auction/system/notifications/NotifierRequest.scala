package auction.system.notifications

import akka.actor._
import auction.system.notifications.AuctionPublisher.ReceivedNotification
import auction.system.notifications.Notifier.{Notification, NotificationPayload}
import auction.system.notifications.NotifierRequest.SuccessfullyDeliveredNotification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by novy on 23.11.15.
  */
class NotifierRequest(notificationToSend: Notification) extends Actor with ActorLogging {

  override def receive: Receive = {
    case ReceivedNotification => notifyParentAboutSuccessfulDeliveryAndKillYourself()
  }

  override def preStart(): Unit = {
    log.debug(s"about to send notification ${notificationToSend.payload}")
    tryToSendNotification()
  }

  def tryToSendNotification(): Unit = {
    val actorSelection: ActorSelection = notificationToSend.target

    actorSelection
      .resolveOne(10 seconds)
      .onComplete(sendNotificationOrThrowException)

    def sendNotificationOrThrowException(possiblyActorRef: Try[ActorRef]): Unit = {
      // this is exactly how you shouldn't use Try and Futures or any other monoid/monad, but it's done intentionally
      // just to throw an exception in case of failure :) (to show how supervising works...)
      val actorRef: ActorRef = possiblyActorRef.get
      actorRef ! notificationToSend.payload
    }
  }

  def notifyParentAboutSuccessfulDeliveryAndKillYourself(): Unit = {
    context.parent ! SuccessfullyDeliveredNotification(notificationToSend.payload)
    context.stop(self)
  }
}

object NotifierRequest {

  def props(notificationToSend: Notification): Props = Props(new NotifierRequest(notificationToSend))

  case class SuccessfullyDeliveredNotification(payload: NotificationPayload)

}
