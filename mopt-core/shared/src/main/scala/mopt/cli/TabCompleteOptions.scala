package mopt.cli

import mopt._
import mopt.annotation._
import mopt.generic._
import mopt.internal.CliParser

case class TabCompleteOptions(
    current: Option[Int] = None,
    format: Option[String] = None,
    arguments: List[String] = Nil
)
object TabCompleteOptions {
  val default: TabCompleteOptions = TabCompleteOptions()
  implicit val surface: Surface[TabCompleteOptions] = new Surface(
    List(
      List(
        new Field(
          "current",
          "Option[Int]",
          List(),
          Nil
        ),
        new Field(
          "format",
          "Option[String]",
          List(),
          Nil
        ),
        new Field(
          "arguments",
          "List[String]",
          List(
            new CatchInvalidFlags(),
            new ExtraName(CliParser.PositionalArgument)
          ),
          Nil
        )
      )
    )
  )
  implicit val encoder: ConfEncoder[TabCompleteOptions] =
    ConfEncoder.StringEncoder
      .contramap[TabCompleteOptions](_.arguments.mkString(" "))
  implicit val decoder: ConfDecoder[TabCompleteOptions] = ConfDecoder.instance {
    case obj: Conf.Obj =>
      (obj.getOption[Int]("current") |@|
        obj.getOption[String]("format") |@|
        obj.get[List[String]]("remainingArgs"))
        .map {
          case ((a, b), c) => TabCompleteOptions(a, b, c)
        }
    case _ =>
      Configured.ok(TabCompleteOptions())
  }
}
