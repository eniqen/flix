package com

/**
 * @author Mikhail Nemenko {@literal <nemenkoma@gmail.com>}
 */
package object boom {
  case class Seat(id: SeatId, status: SeatStatus)

  case class SeatId(value: Int) extends AnyVal

  sealed trait SeatStatus extends Product with Serializable
  case object Free extends SeatStatus
  case object Busy extends SeatStatus

  case class Route(segments: Map[Segment, List[Seat]])
  case class Segment(from: Station, to: Station)
  case class SegmentInfo(id: Segment, seats: List[Seat])
  case class Relations(connected: List[Station])

  object Relations {
    def getSegments(c: Relations): Set[Segment] = c.connected.zip(c.connected.tail).map((Segment.apply _).tupled).toSet
  }

  sealed trait Station extends Product with Serializable
  case object Berlin    extends Station
  case object Warsaw  extends Station
  case object Tirana    extends Station
  case object Ioannina  extends Station
  case object Litochoro extends Station
}
