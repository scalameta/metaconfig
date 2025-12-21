package metaconfig

import metaconfig.generic.Settings

trait ConfCodec[A] extends ConfDecoder[A] with ConfEncoder[A] { self =>
  def bimap[B](in: B => A, out: A => B): ConfCodec[B] = new ConfCodec[B] {
    override def write(value: B): Conf = self.write(in(value))
    override def read(conf: Conf): Configured[B] = self.read(conf).map(out)
    override def convert(conf: Conf): Conf = self.convert(conf)
  }
}

object ConfCodec {
  def apply[A](implicit ev: ConfCodec[A]): ConfCodec[A] = ev

  private[metaconfig] class Pair[A](
      private[metaconfig] val encoder: ConfEncoder[A],
      private[metaconfig] val decoder: ConfDecoder[A],
  ) extends ConfCodec[A] {
    override def write(value: A): Conf = encoder.write(value)
    override def read(conf: Conf): Configured[A] = decoder.read(conf)
    override def convert(conf: Conf): Conf = decoder.convert(conf)

    private[metaconfig] def getPair(): (ConfEncoder[A], ConfDecoder[A]) =
      (encoder, decoder)
  }

  implicit def EncoderDecoderToCodec[A](implicit
      encode: ConfEncoder[A],
      decode: ConfDecoder[A],
  ): ConfCodec[A] = new Pair(encode, decode)

  val IntCodec: ConfCodec[Int] = ConfCodec[Int]
  val StringCodec: ConfCodec[String] = ConfCodec[String]
  val BooleanCodec: ConfCodec[Boolean] = ConfCodec[Boolean]

  implicit final class Implicits[A](private val self: ConfCodec[A])
      extends AnyVal {

    private[metaconfig] def getPair(): (ConfEncoder[A], ConfDecoder[A]) =
      self match {
        case _: Pair[_] => self.asInstanceOf[Pair[A]].getPair()
        case _ => (self, self)
      }

    private[metaconfig] def withDecoder(
        f: ConfDecoder[A] => ConfDecoder[A],
    ): ConfCodec[A] = {
      val (encoder, decoder) = getPair()
      val dec = f(decoder)
      if (dec eq decoder) self else new ConfCodec.Pair(encoder, dec)
    }

    def detectSectionRenames(implicit settings: Settings[A]): ConfCodec[A] =
      self.withDecoder(_.detectSectionRenames)

    def withSectionRenames(renames: annotation.SectionRename*): ConfCodec[A] =
      self.withDecoder(_.withSectionRenames(renames: _*))

  }

  def oneOf[A](options: sourcecode.Text[A]*): ConfCodec[A] =
    oneOfCustom[A](options: _*)(PartialFunction.empty)

  def oneOfCustom[A](
      options: sourcecode.Text[A]*,
  )(fconv: PartialFunction[Conf, Conf]): ConfCodec[A] = ConfEnum
    .oneOfCustom[A, ConfDecoder, ConfCodec](options: _*) { f =>
      ConfDecoder.fromPartialConverted[A]("String")(fconv) { case x: Conf.Str =>
        f(x)
      }
    }(new Pair(_, _))

}
