package metaconfig.cli

import metaconfig.Conf.Obj
import metaconfig.annotation.ExtraName
import metaconfig.generic.{Field, Surface}
import metaconfig.{Conf, ConfDecoder, ConfEncoder, Configured}

case class HelpOptions(
    @ExtraName("remainingArgs")
    subcommand: List[String] = Nil,
)
// NOTE(olafur) We manually write surface and codecs because we can't use macros
// in the same project as where we define macros. The boilerplat is not nice at
// all.
object HelpOptions {
  val default: HelpOptions = HelpOptions()
  implicit val surface: Surface[HelpOptions] = new Surface(List(List(new Field(
    "subcommand",
    "List[String]",
    List(new ExtraName("remainingArgs")),
    Nil,
  ))))
  implicit val encoder: ConfEncoder[HelpOptions] = ConfEncoder.StringEncoder
    .contramap[HelpOptions](_.subcommand.mkString(" "))
  implicit val decoder: ConfDecoder[HelpOptions] = ConfDecoder.from {
    case Obj(List(("remainingArgs", Conf.Lst(subcommands)))) => Configured
        .ok(HelpOptions(subcommands.collect { case Conf.Str(command) =>
          command
        }))
    case _ => Configured.ok(HelpOptions(Nil))
  }
}
