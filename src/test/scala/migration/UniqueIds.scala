package migration

import java.util.concurrent.atomic.AtomicLong

object UniqueIds {
  private val unique = new AtomicLong(System.currentTimeMillis())

  def next(): Long = unique.incrementAndGet()
}
