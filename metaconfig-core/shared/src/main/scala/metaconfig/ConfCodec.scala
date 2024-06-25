package metaconfig

trait ConfCodec[A] extends ConfDecoder[A] with ConfEncoder[A] { self =>
  def bimap[B](in: B => A, out: A => B): ConfCodec[B] = new ConfCodec[B] {
    override def write(value: B): Conf = self.write(in(value))
    override def read(conf: Conf): Configured[B] = self.read(conf).map(out)
  }
}

object ConfCodec {
  def apply[A](implicit ev: ConfCodec[A]): ConfCodec[A] = ev
  implicit def EncoderDecoderToCodec[A](implicit
      encode: ConfEncoder[A],
      decode: ConfDecoder[A]
  ): ConfCodec[A] = new ConfCodec[A] {
    override def write(value: A): Conf = encode.write(value)
    override def read(conf: Conf): Configured[A] = decode.read(conf)
  }

  val IntCodec: ConfCodec[Int] = ConfCodec[Int]
  val StringCodec: ConfCodec[String] = ConfCodec[String]
  val BooleanCodec: ConfCodec[Boolean] = ConfCodec[Boolean]

}
