package metaconfig.internal

import metaconfig._

object NoTyposDecoder {

  def apply[A: generic.Settings](dec: ConfDecoder[A]): ConfDecoder[A] =
    if (dec.isInstanceOf[Decoder[_]]) dec else new Decoder[A](dec)

  def apply[S, A: generic.Settings](
      dec: ConfDecoderExT[S, A],
  ): ConfDecoderExT[S, A] =
    if (dec.isInstanceOf[DecoderEx[_, _]]) dec else new DecoderEx[S, A](dec)

  private def checkTypos[A](conf: Conf, otherwise: => Configured[A])(implicit
      ev: generic.Settings[A],
  ): Configured[A] = ConfDecoder
    .readWithPartial("Object") { case Conf.Obj(values) =>
      val names = ev.allNames
      val typos = values
        .collect { case (key, obj) if !names.contains(key) => key -> obj.pos }
      ConfError.invalidFieldsOpt(typos, ev.nonHiddenNames)
        .fold(otherwise)(_.notOk)
    }(conf)

  private class Decoder[A: generic.Settings](dec: ConfDecoder[A])
      extends ConfDecoder[A] {
    override def read(conf: Conf): Configured[A] =
      checkTypos(conf, dec.read(conf))
  }

  private class DecoderEx[-S, A: generic.Settings](dec: ConfDecoderExT[S, A])
      extends ConfDecoderExT[S, A] {
    override def read(state: Option[S], conf: Conf): Configured[A] =
      checkTypos(conf, dec.read(state, conf))
  }

}
