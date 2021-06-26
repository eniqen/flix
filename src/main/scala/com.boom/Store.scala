package com.boom

import cats.Functor
import cats.effect.Ref
import cats.effect.kernel.Ref.Make
import cats.implicits.toTraverseOps
import cats.implicits._

/**
 * @author Mikhail Nemenko {@literal <nemenkoma@gmail.com>}
 */
trait Store[F[_]] {
  def update(fn: Route => Route): F[Unit]
  def reserveSeatsForRel(rel: Relations): F[List[(Segment, SeatId)]]
  def releaseSeatsFor(data: (Segment, SeatId)*): F[Unit]
  def getAvailableSeatsFor(segment: Segment): F[SegmentInfo]
  def getState: F[Route]
}

final class InmemoryStore[F[_]: Functor] private(inner: Ref[F, Route]) extends Store[F] {
  override def update(fn: Route => Route): F[Unit] = inner.update(fn)

  override def getAvailableSeatsFor(s: Segment): F[SegmentInfo] = inner.get.map {
    _.segments.get(s).fold(SegmentInfo(s, List.empty))(seats => SegmentInfo(s, seats.filter(_.status == Free)))
  }

  override def getState: F[Route] = inner.get

  override def reserveSeatsForRel(rel: Relations): F[List[(Segment, SeatId)]] = inner.modify {
    route =>

      def getFirstAvailableAndRest(segment: Segment)(seats: List[Seat]): Option[(Segment, Seat, List[Seat])] = {
        val toAdd   = seats.collectFirst { case seat if seat.status == Free => seat}
        val updated = toAdd.fold(seats)(s => seats.filterNot(_.id.value == s.id.value))
        toAdd.map(seat => (segment, seat, updated))
      }

      val updatedResult = Relations.getSegments(rel).toList.map { segment =>
        route.segments.get(segment).flatMap(getFirstAvailableAndRest(segment)(_))
      }.sequence

      updatedResult.fold(route -> List.empty[(Segment, SeatId)]) {
        result =>
          result.foldLeft(route -> List.empty[(Segment, SeatId)]) {
            case ((route, bookedSegmentsSeats), (seg, bookedSeat, restSeats)) =>
              route.copy(segments = route.segments.updated(seg, bookedSeat.copy(status = Busy) :: restSeats)) -> ((seg, bookedSeat.id) :: bookedSegmentsSeats)
          }.map(_.reverse)
      }
  }

  override def releaseSeatsFor(data: (Segment, SeatId)*): F[Unit] = update {
    route =>
      data.foldLeft(route) {
        case (acc, (segment, seatId)) =>
          acc.copy(segments = acc.segments.get(segment).fold(acc.segments)
          (forUpdate => acc.segments.updated(segment, Seat(seatId, Free) :: forUpdate)))
      }
  }
}

object InmemoryStore {
  def make[F[_]: Functor: Make](route: Route): F[Store[F]] = Ref.of[F, Route](route).map(new InmemoryStore[F](_)).widen
}