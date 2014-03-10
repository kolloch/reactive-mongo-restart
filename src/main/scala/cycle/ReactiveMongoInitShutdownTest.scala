package cycle

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import scala.concurrent.Await
import scala.concurrent.duration._
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory}

object ReactiveMongoInitShutdownTest {
  private val logger = LoggerFactory.getLogger(getClass)

  def completeCycle() {
    val system = ActorSystem()

    import system.dispatcher

    val driver = MongoDriver(system)
    val connection = driver.connection(nodes = Seq("localhost"))
    val db = connection.db("test")

    logger.info("before waitForPrimaryResult")

    val waitForPrimaryResult = Await.result(connection.waitForPrimary(3.seconds), 3.seconds)

    logger.info("waitForPrimaryResult: {}", waitForPrimaryResult)

    val collection: BSONCollection = db.collection("blubber")

    val dbStuffFuture = collection.insert(BSONDocument("_id" -> 1, "hi" -> "stuff"))
      .flatMap { _ =>
      collection.find(BSONDocument("_id" -> 1)).one[BSONDocument].map { resultDoc =>
        require(resultDoc.get.getAs[String]("hi").get == "stuff", "unexpected document content: " + resultDoc.get.getAs[String]("hi"))
      }
    }

    Await.result(dbStuffFuture, 3.seconds)

    logger.info("before askCloseResult")

    val askCloseResult = Await.result(connection.askClose()(3.seconds), 3.seconds)

    logger.info("askCloseResult: {}", askCloseResult)

    logger.info("before actor system shutdown")
    system.shutdown()
    system.awaitTermination()
    logger.info("before actor system shutdown: done.")
  }

  def main(args: Array[String]) {
    InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

    for (_ <- 1 to 100) {
      completeCycle()
    }
  }
}
