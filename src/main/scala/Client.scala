import Client._
import MyPassBall.{ system}
import akka.actor.{Actor, ActorRef}
import Server._
import akka.pattern.ask
import akka.remote.DisassociatedEvent
import akka.util.Timeout
import scalafx.application.Platform

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Client extends Actor {
  implicit val mytimeout = new Timeout(10 seconds)
  context.system.eventStream.subscribe(self, classOf[akka.remote.DisassociatedEvent])
  def receive ={
    case StartJoin(server, port, name)=>
      val serverRef = context.actorSelection(s"akka.tcp://ball@$server:$port/user/server")
      val result  = serverRef ? Join(self, name)
      result.foreach( x => {
        if(x == Client.Joined){
          Platform.runLater {
            MyPassBall.controller.displayJoinStatus("You have Joined")
          }
          context.become(joined)
        } else {
          Platform.runLater {
            MyPassBall.controller.displayJoinStatus("Error")
          }
        }
      })
    case _=>
  }
  def joined: Receive = {
    case StartJoin(x, y, z)=>
      Platform.runLater {
        MyPassBall.controller.displayJoinStatus("You have already Joined")
      }
    case Begin =>
      Platform.runLater {
        MyPassBall.controller.hideBall()
      }
      sender ! "done"
    case Take =>
      Platform.runLater {
        MyPassBall.controller.showBall()
      }
      sender ! Taken
    case PassBall=>

      for (iter <- Client.memberIter){
        val clients = iter.take(1).toList
        for (client <- clients){
          val f = client ? Take
          f foreach(x => {
            Platform.runLater {
              MyPassBall.controller.hideBall()
            }
          })
        }
      }
    case Members(list)=>
      Client.memberList = Option(list.filterNot(x => x.ref == self))
      Client.memberIter = Option(Iterator.continually(Client.memberList.get.map(_.ref)).flatten)
      Platform.runLater{
        MyPassBall.controller.displayMemberList(memberList.getOrElse(List()).toList)
      }
    case DisassociatedEvent(local, remote, _) =>
      MyPassBall.showErrorDialog(s"$remote has been disconnected, game ended")
      context.unbecome()
    case _=>
      println("discard message")
  }
}
object Client {
  var memberList: Option[Iterable[Person]] = None
  var memberIter: Option[Iterator[ActorRef]] = None
  //temporry
  case object Joined
  case class StartJoin(serverIp: String, port: String,name: String)
  case object Begin
  case object Take
  case object PassBall
  case class Members(member: Iterable[Person])
}