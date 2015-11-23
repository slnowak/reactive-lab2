package auction.system.notifications

import akka.actor.Actor
import auction.system.notifications.Notifier.Notification

/**
  * Created by novy on 23.11.15.
  */
class NotifierRequest(notificationToSend: Notification) extends Actor {

  override def receive: Receive = ???
}
