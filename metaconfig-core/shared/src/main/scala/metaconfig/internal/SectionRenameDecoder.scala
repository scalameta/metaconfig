package metaconfig.internal

import metaconfig._
import metaconfig.generic.Settings

import scala.annotation.tailrec

trait SectionRenameDecoder[A] extends Transformable[A] { self: A =>
  protected val renames: List[annotation.SectionRename]
  protected def renameSectionsAnd[B](
      conf: Conf,
      func: Conf => Configured[B],
  ): Configured[B] = SectionRenameDecoder.renameSections(renames)(conf)
    .andThen(func)
}

object SectionRenameDecoder {

  @tailrec
  def apply[A](
      dec: ConfDecoder[A],
      renames: List[annotation.SectionRename],
  ): ConfDecoder[A] = dec match {
    case x: Decoder[_] =>
      if (x.renames eq renames) dec else apply(x.dec, x.renames ++ renames)
    case _ => new Decoder[A](dec, renames.distinct)
  }

  @tailrec
  def apply[S, A](
      dec: ConfDecoderExT[S, A],
      renames: List[annotation.SectionRename],
  ): ConfDecoderExT[S, A] = dec match {
    case x: DecoderEx[_, _] =>
      if (x.renames eq renames) dec
      else apply(x.asInstanceOf[DecoderEx[S, A]].dec, x.renames ++ renames)
    case _ => new DecoderEx[S, A](dec, renames.distinct)
  }

  def apply[A](dec: ConfDecoder[A])(implicit ev: Settings[A]): ConfDecoder[A] =
    fromSettings(ev, dec)(apply[A])

  def apply[S, A](dec: ConfDecoderExT[S, A])(implicit
      ev: Settings[A],
  ): ConfDecoderExT[S, A] = fromSettings(ev, dec)(apply[S, A])

  private def fromSettings[D](ev: Settings[_], obj: D)(
      f: (D, List[annotation.SectionRename]) => D,
  ): D = {
    val list = ev.annotations.collect { case x: annotation.SectionRename => x }
    if (list.isEmpty) obj else f(obj, list)
  }

  private class Decoder[A](
      val dec: ConfDecoder[A],
      val renames: List[annotation.SectionRename],
  ) extends ConfDecoder[A] with SectionRenameDecoder[ConfDecoder[A]] {
    override def read(conf: Conf): Configured[A] =
      renameSectionsAnd(conf, dec.read)
    override def transform(f: SelfType => SelfType): SelfType =
      apply(f(dec), renames)
  }

  private class DecoderEx[S, A](
      val dec: ConfDecoderExT[S, A],
      val renames: List[annotation.SectionRename],
  ) extends ConfDecoderExT[S, A] with SectionRenameDecoder[ConfDecoderExT[S, A]] {
    override def read(state: Option[S], conf: Conf): Configured[A] =
      renameSectionsAnd(conf, dec.read(state, _))
    override def transform(f: SelfType => SelfType): SelfType =
      apply(f(dec), renames)
  }

  @tailrec
  private def renameSections(
      values: List[annotation.SectionRename],
  )(conf: Conf): Configured[Conf] = values match {
    case head :: rest =>
      val oldName = head.oldNameAsSeq
      conf.getNestedConf(oldName: _*) match {
        case Configured.Ok(oldVal: Conf) =>
          val del = Conf.Obj.empty.nestedWithin(oldName: _*)
          val add = oldVal.nestedWithin(head.newNameAsSeq: _*)
          // remove on right (takes precedence), append on left (doesn't)
          renameSections(rest)(ConfOps.merge(add, ConfOps.merge(conf, del)))
        case _ => renameSections(rest)(conf)
      }
    case _ => Configured.Ok(conf)
  }

}
