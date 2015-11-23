package auction.system.notifications

import akka.actor._
import auction.system.notifications.AuctionPublisher.ReceivedNotification
import auction.system.notifications.Notifier.{Notification, NotificationPayload}
import auction.system.notifications.NotifierRequest.SuccessfullyDeliveredNotification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by novy on 23.11.15.
  */
class NotifierRequest(notificationToSend: Notification) extends Actor with ActorLogging {

  override def receive: Receive = {
    case ReceivedNotification => notifyParentAboutSuccessfulDeliveryAndKillYourself()
    case exception: Throwable => throw exception
  }

  override def preStart(): Unit = {
    log.debug(s"about to send notification ${notificationToSend.payload}")
    tryToSendNotification()
  }

  def tryToSendNotification(): Unit = {
    val actorSelection: ActorSelection = notificationToSend.target

    val eventualActorRef: Future[ActorRef] = actorSelection.resolveOne(10 seconds)
    eventualActorRef.onSuccess(sendNotification)
    eventualActorRef.onFailure(rethrowException)

    def sendNotification: PartialFunction[ActorRef, Unit] = {
      case ref: ActorRef => ref ! notificationToSend.payload
    }

    // since promise is resolved in different thread, we have to resend and rethrow exception so it's caught by supervisor
    def rethrowException: PartialFunction[Throwable, Unit] = {
      case ex: Throwable => self ! ex
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
