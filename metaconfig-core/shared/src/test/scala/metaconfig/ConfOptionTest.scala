package metaconfig

import org.scalameta.logger
import org.scalatest.FunSuite

object CustomMessage {
  def decoder[T](field: String)(implicit decoder: ConfDecoder[T]): ConfDecoder[CustomMessage[T]] =
    ConfDecoder.instance[CustomMessage[T]] {
      case obj: Conf.Obj =>
        (obj.get[T](field) |@| 
          obj.getOption[String]("message")).map{ case (value, message) =>
          CustomMessage(value, message)
        }
      case raw => 
        decoder.read(raw).map(value => CustomMessage(value, None))
    }
}
case class CustomMessage[T](value: T, message: Option[String])
case class DisableConfig(symbols: List[CustomMessage[String]] = Nil) {
  implicit class XtensionConfScalafix(conf: Conf) {
    def getField[T: ConfDecoder](e: sourcecode.Text[T]): Configured[T] =
      conf.getOrElse(e.source)(e.value)
  }

  implicit val customMessageReader: ConfDecoder[CustomMessage[String]] =
    CustomMessage.decoder(field = "symbol")

  implicit val reader: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig](
      _.getField(symbols).map(DisableConfig(_))
    )
}

object DisableConfig {
  val default = DisableConfig()
  implicit val reader: ConfDecoder[DisableConfig] = default.reader
}

class ConfOptionTest extends FunSuite {
  def decode[T](conf: Conf)(implicit decoder: ConfDecoder[T]): T = 
    decoder.read(conf).get

  import Conf._

  test("all") {
    val conf = Obj(
      "symbols" -> Lst(
        Str("scala.Any.asInstanceOf"),
        Obj(
          "symbol" -> Str("scala.Option.get")
        ),
        Obj(
          "symbol" -> Str("scala.List.get"),
          "message" -> Str("You should not use List.get")
        )
      )
    )
    val obtained = decode[DisableConfig](conf)
    val expected = DisableConfig(
      symbols = List(
        CustomMessage(
          value = "scala.Any.asInstanceOf",
          message = None
        ),
        CustomMessage(
          value = "scala.Option.get",
          message = None
        ),
        CustomMessage(
          value = "scala.List.get",
          message = Some("You should not use List.get")
        )
      )
    )
    assert(obtained == expected)
  }
}