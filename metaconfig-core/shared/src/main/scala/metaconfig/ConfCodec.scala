package metaconfig

trait ConfCodec[A] extends ConfDecoder[A] with ConfEncoder[A] { self =>
  def bimap[B](in: B => A, out: A => B): ConfCodec[B] =
    new ConfCodec.Pair[B](encoder.contramap(in), decoder.map(out))

  private[metaconfig] def decoder: ConfDecoder[A]
  private[metaconfig] def encoder: ConfEncoder[A]

}

object ConfCodec {
  def apply[A](implicit ev: ConfCodec[A]): ConfCodec[A] = ev

  private[metaconfig] class Pair[A](
      private[metaconfig] val encoder: ConfEncoder[A],
      private[metaconfig] val decoder: ConfDecoder[A],
  ) extends ConfCodec[A] {
    override def write(value: A): Conf = encoder.write(value)
    override def read(conf: Conf): Configured[A] = decoder.read(conf)
  }

  implicit def EncoderDecoderToCodec[A](implicit
      encode: ConfEncoder[A],
      decode: ConfDecoder[A],
  ): ConfCodec[A] = new Pair(encode, decode)

  val IntCodec: ConfCodec[Int] = ConfCodec[Int]
  val StringCodec: ConfCodec[String] = ConfCodec[String]
  val BooleanCodec: ConfCodec[Boolean] = ConfCodec[Boolean]

}
