package com.boom

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits.toTraverseOps
import cats.syntax.all._

/**
 * @author Mikhail Nemenko {@literal <nemenkoma@gmail.com>}
 */
object MainApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = runApp[IO].flatMap(_ => IO.never).as(ExitCode.Success)

  private def runApp[F[_]: Sync]: F[Unit] = {

    val relations = Relations(List(Berlin, Tirana, Ioannina, Litochoro))
    val segments  = Relations.getSegments(relations)
    def buildSeats(min: Int = 1, max: Int = 5): List[Seat] = (min to max).map(id => Seat(SeatId(id), Free)).toList
    val route = Route(
      segments.map(_ -> buildSeats(1, 3)).toMap,
    )

    for {
      store <- InmemoryStore.make[F](route)

      segmentsInfo <- segments.toList.map(store.getAvailableSeatsFor).sequence
      _ <- Sync[F].delay(println(
        s"""
           |======================================
           | INITIAL RESULT
           | ${segmentsInfo.mkString("\n")}
           |======================================
           |""".stripMargin))

      reservedSeats <- store.reserveSeatsForRel(relations)
      _ <- Sync[F].delay(println(s"Reserved Seats = $reservedSeats"))

      segmentsInfo <- segments.toList.map(store.getAvailableSeatsFor).sequence
      _ <- Sync[F].delay(println(
        s"""
           |======================================
           | Avialabile After Booking
           | ${segmentsInfo.mkString("\n")}
           |======================================
           |""".stripMargin))

      _ <- store.getState.flatMap(route => Sync[F].delay(println(s"Full Info ${route.segments.mkString("\n")}\n")))

      onlyForOneSegment <- store.reserveSeatsForRel(Relations(List(Tirana, Ioannina)))

      _ <- Sync[F].delay(println(s"RESERVED FOR ONE SEGMENT $onlyForOneSegment"))

      segmentsInfo <- segments.toList.map(store.getAvailableSeatsFor).sequence
      _ <- Sync[F].delay(println(
        s"""
           |======================================
           | Avialability After One Segment Booking
           | ${segmentsInfo.mkString("\n")}
           |======================================
           |""".stripMargin))

    } yield ()
  }
}