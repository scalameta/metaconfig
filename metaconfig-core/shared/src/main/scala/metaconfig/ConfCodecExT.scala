package metaconfig

import metaconfig.generic.Settings

class ConfCodecExT[S, A](encoder: ConfEncoder[A], decoder: ConfDecoderExT[S, A])
    extends ConfDecoderExT[S, A] with ConfEncoder[A] {
  override def write(value: A): Conf = encoder.write(value)
  override def read(state: Option[S], conf: Conf): Configured[A] = decoder
    .read(state, conf)

  def bimap[B](in: B => A, out: A => B): ConfCodecExT[S, B] =
    new ConfCodecExT[S, B](encoder.contramap(in), decoder.map(out))

  def noTypos(implicit settings: Settings[A]): ConfCodecExT[S, A] = {
    val noTyposDecoder = decoder.noTypos
    if (noTyposDecoder eq decoder) this
    else new ConfCodecExT(encoder, noTyposDecoder)
  }

}

object ConfCodecExT {
  def apply[A, B](implicit ev: ConfCodecExT[A, B]): ConfCodecExT[A, B] = ev
}

object ConfCodecEx {
  def apply[A](implicit obj: ConfCodecEx[A]): ConfCodecEx[A] = obj
}
