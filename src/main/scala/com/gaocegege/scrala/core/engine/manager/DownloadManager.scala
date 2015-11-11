package com.gaocegege.scrala.core.engine.manager

import akka.actor.Actor
import com.gaocegege.scrala.core.downloader.Downloader
import scala.collection.mutable
import com.gaocegege.scrala.core.downloader.impl.HttpDownloader
import akka.actor.{ Props, ActorRef }
import com.gaocegege.scrala.core.common.request.Request
import scala.util.Random
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import com.gaocegege.scrala.core.common.util.Constant
import com.gaocegege.scrala.core.engine.manager.status.Status

/**
 * Downloader manager
 * @author gaoce
 */
class DownloadManager(engine: ActorRef, val threadCount: Int = 4) extends Actor {

  private val logger = Logger(LoggerFactory.getLogger("downloadmanager"))

  /** children */
  private val workers: mutable.ListBuffer[ActorRef] = new mutable.ListBuffer[ActorRef]()

  for (i <- 1 to threadCount) {
    workers.append(context.actorOf(Props[HttpDownloader], "worker-" + i.toString()))
  }

  private val states: mutable.ListBuffer[Status.Value] = mutable.ListBuffer.fill(threadCount)(Status.Done)

  /**
   * request, work; end, tell me.
   */
  def receive = {
    case request: Request => {
      val index = Random.nextInt(threadCount)
      states(index) = Status.Working
      workers(index) ! (request, index)
    }
    case (Constant.endMessage, index: Int) => {
      states(index) = Status.Done
      if (IsAllDone()) {
        // TODO send to scheduler
        engine ! Constant.endMessage
      }
    }
    case _ => {
      logger.warn("[DownloadManager]-unexpected message")
    }
  }

  def IsAllDone(): Boolean = {
    states.forall { ele => ele == Status.Done }
  }
}
