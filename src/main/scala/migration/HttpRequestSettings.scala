package migration

import scala.concurrent.duration.FiniteDuration

case class HttpRequestSettings(
    readTimeout: FiniteDuration,
    connectionTimeout: FiniteDuration,
    acceptableStatuses: Set[Int] = Set.empty
)
