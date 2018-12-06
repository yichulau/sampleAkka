import Client.{Begin, Joined, Members, Take}
import Server.{Join, Start, Taken}
import akka.actor.{Actor, ActorRef}
import scalafx.collections.ObservableHashSet
import akka.pattern.ask
import akka.remote.DisassociatedEvent
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

case class Person(ref: ActorRef, name: String){
  override def toString: String = {
    name
  }
}
class Server extends Actor{
  implicit val timeout: Timeout = Timeout(10 second)
  context.system.eventStream.subscribe(self, classOf[akka.remote.DisassociatedEvent])
  def receive = {
    case Join(my, name) =>
      Server.players += new Person(my, name)
      sender() ! Joined
    case Start=>
      context.become(started)
      val results = for(client <- Server.players) yield {
        client.ref ? Begin
      }
      val begintasks = Future {
        var i = 0
        for (result <- results) {
          for (value <- result) {
            i = i + 1
          }
        }
      }

      //pass a ball to the client
      begintasks foreach { x=>
        val iter = Iterator.continually(Server.players.map(x=>x.ref)).flatten
        val clients  = iter.take(1).toList
        for (client <- clients){
          val f = client ? Take
        }
        for (client <- Server.players.map(_.ref)){
          client ! Members(Server.players.toList)
        }
      }
    case _=>
  }
  def started: Receive = {
    case DisassociatedEvent(local, remote,_) =>
      context.unbecome()
      Server.players.clear()
    case _=>
  }
}
object Server {
  val players = new ObservableHashSet[Person]
  //ask
  case class Join(myref: ActorRef, name: String)
  case object Start
  case object Taken
}