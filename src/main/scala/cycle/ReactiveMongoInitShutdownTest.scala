package cycle

import akka.actor.ActorSystem
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import reactivemongo.utils.LazyLogger
import scala.concurrent.Await
import scala.concurrent.duration._

object ReactiveMongoInitShutdownTest {
  private val logger = LazyLogger("ReactiveMongoInitShutdownTest")

  def completeCycle(i: Int) {
    val system = ActorSystem()

    import system.dispatcher

    val driver = MongoDriver(system)
    val connection = driver.connection(nodes = Seq("localhost"))
    val db = connection.db("test")

    println("before waitForPrimaryResult")

    val waitForPrimaryResult = Await.result(connection.waitForPrimary(3.seconds), 3.seconds)

    println(s"[cycle #$i] waitForPrimaryResult: $waitForPrimaryResult")

    val collection: BSONCollection = db.collection("blubber")

    val dbStuffFuture = collection.insert(BSONDocument("_id" -> 1, "hi" -> "stuff"))
      .flatMap { _ =>
        collection.find(BSONDocument("_id" -> 1)).one[BSONDocument] }

    val result = Await.result(dbStuffFuture, 3.seconds)
    require(result.isDefined, "should have found a doc")
    require(result.get.getAs[String]("hi").exists(_ == "stuff"), "unexpected document content: " + result.get.getAs[String]("hi"))

    Await.result(collection.remove(BSONDocument("_id" -> 1)), 3.seconds)

    logger.info("[cycle #$i] before askCloseResult")

    val askCloseResult = Await.result(connection.askClose()(3.seconds), 3.seconds)

    logger.info(s"askCloseResult: $askCloseResult")

    logger.info("before actor system shutdown")
    system.shutdown()
    system.awaitTermination()
    logger.info("before actor system shutdown: done.")
  }

  def main(args: Array[String]) {

    for(j <- 1 to 100) {
      println(s"~~~~~ Runnning Test #$j ~~~~~")
      for (i <- 1 to 100) {
        completeCycle(j * i)
      }
    }
  }
}
