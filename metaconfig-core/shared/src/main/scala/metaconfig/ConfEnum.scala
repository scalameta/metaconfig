package metaconfig

class ConfEnum[A] private (val map: Map[String, A]) extends AnyVal {
  def unapply(tag: String): Option[A] = map
    .get(ConfEnum.lowerCaseNoBackticks(tag))
}

object ConfEnum {
  def apply[A](opts: sourcecode.Text[A]*): ConfEnum[A] =
    new ConfEnum(opts.map(x => lowerCaseNoBackticks(x.source) -> x.value).toMap)

  def lowerCaseNoBackticks(s: String): String = s.toLowerCase().replace("`", "")

  def oneOfCustom[A, D[_], C[_]](options: sourcecode.Text[A]*)(
      f: (Conf.Str => Configured[A]) => D[A],
  )(c: (ConfEncoder[A], D[A]) => C[A]): C[A] = {
    val enums: ConfEnum[A] = ConfEnum(options: _*)
    val decoder = f { case Conf.Str(x) =>
      def msg = enums.map.keys
        .mkString(s"Invalid '$x'; expected one of: ", ", ", "")
      Configured.opt(enums.unapply(x))(ConfError.message(msg))
    }
    val encoder = ConfEncoder.instance[A](value =>
      options.collectFirst { case sourcecode.Text(`value`, source) =>
        Conf.Str(source)
      }.getOrElse(Conf.Null()),
    )
    c(encoder, decoder)
  }

}
