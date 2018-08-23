package actors

import akka.actor._

import scala.collection.mutable.ListBuffer

object MyWebSocketActor {
  var pool = ListBuffer[MyWebSocketActor]()

  def broadcast(s: String): Unit = {
    pool.foreach(_.send(s))
  }

  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  MyWebSocketActor.pool += this
  println("actor " + MyWebSocketActor.pool.size)
  def send(s: String): Unit = {
    out ! (s)
  }

  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }
}