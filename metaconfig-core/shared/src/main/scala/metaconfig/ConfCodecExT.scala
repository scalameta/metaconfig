package metaconfig

import metaconfig.generic.Settings

class ConfCodecExT[S, A](encoder: ConfEncoder[A], decoder: ConfDecoderExT[S, A])
    extends ConfDecoderExT[S, A] with ConfEncoder[A] {
  override def write(value: A): Conf = encoder.write(value)
  override def read(state: Option[S], conf: Conf): Configured[A] = decoder
    .read(state, conf)

  def bimap[B](in: B => A, out: A => B): ConfCodecExT[S, B] =
    new ConfCodecExT[S, B](encoder.contramap(in), decoder.map(out))

  def withDecoder(
      f: ConfDecoderExT[S, A] => ConfDecoderExT[S, A],
  ): ConfCodecExT[S, A] = {
    val dec = f(decoder)
    if (dec eq decoder) this else new ConfCodecExT(encoder, dec)
  }

  def noTypos(implicit settings: Settings[A]): ConfCodecExT[S, A] =
    withDecoder(_.noTypos)

  override def convert(conf: Conf): Conf = decoder.convert(conf)
}

object ConfCodecExT {
  def apply[A, B](implicit ev: ConfCodecExT[A, B]): ConfCodecExT[A, B] = ev

  implicit final class Implicits[S, A](private val self: ConfCodecExT[S, A])
      extends AnyVal {

    def detectSectionRenames(implicit
        settings: Settings[A],
    ): ConfCodecExT[S, A] = self.withDecoder(_.detectSectionRenames)

    def withSectionRenames(
        renames: annotation.SectionRename*,
    ): ConfCodecExT[S, A] = self.withDecoder(_.withSectionRenames(renames: _*))

  }

}

object ConfCodecEx {
  def apply[A](implicit obj: ConfCodecEx[A]): ConfCodecEx[A] = obj
}
