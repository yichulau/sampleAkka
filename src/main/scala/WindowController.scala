import Client.{PassBall, StartJoin}
import Server.Start
import akka.actor.ActorRef
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, ListView, TextField}
import scalafx.scene.shape.Circle
import scalafxml.core.macros.sfxml

@sfxml
class WindowController(private val serverIP: TextField,
                       private val port: TextField,
                       private val joinStatusLabel: Label,
                       private val clientList: ListView[Person],
                       private val ball: Circle,
                       private val btnPass: Button,
                       private val txtName: TextField,
                       private val playerList: ListView[Person]) {
  //iniitialize by app
  var clientActorRef: Option[ActorRef] = None

  Server.players.onChange((x, y) => {
    Platform.runLater {
      clientList.items = ObservableBuffer(x.toList)
    }
  })

  def handleStart(actionEvent: ActionEvent): Unit ={
    MyPassBall.serverRef ! Start
  }

  def handleJoin(actionEvent: ActionEvent) {
    //ask the client actor to joined the server based on IP
    clientActorRef foreach { ref =>
      ref ! StartJoin(serverIP.text.value, port.text.value, txtName.text.value)
    }
  }

  def handlePass(actionEvent: ActionEvent): Unit ={
    MyPassBall.clientRef ! PassBall
  }
  def displayJoinStatus(text: String): Unit = {
    joinStatusLabel.text = text
  }
  def hideBall(): Unit ={
    ball.visible = false
    btnPass.disable = true
  }
  def showBall(): Unit ={
    ball.visible = true
    btnPass.disable = false
  }
  def displayMemberList(x: List[Person]): Unit ={
    playerList.items = ObservableBuffer(x)
  }
}
