package metaconfig.annotation

import metaconfig.Conf

import scala.annotation.StaticAnnotation
import scala.collection.compat.immutable.ArraySeq
import scala.language.implicitConversions

import org.typelevel.paiges.Doc

final case class ShortName(value: Char) extends StaticAnnotation
final case class ExtraName(value: String) extends StaticAnnotation
final case class DeprecatedName(
    name: String,
    message: String,
    sinceVersion: String,
) extends StaticAnnotation {
  override def toString: String =
    s"Setting '$name' is deprecated since version $sinceVersion. $message"
}
final case class ExampleValue(value: String) extends StaticAnnotation
final case class BinaryName(value: String) extends StaticAnnotation
final case class Usage(value: String) extends StaticAnnotation
final case class Description(value: String) extends StaticAnnotation
final case class ExampleUsage(value: String) extends StaticAnnotation
final case class DescriptionDoc(value: Doc) extends StaticAnnotation
final case class SinceVersion(value: String) extends StaticAnnotation
final case class Deprecated(message: String, since: String)
    extends StaticAnnotation
final case class Repeated() extends StaticAnnotation
final case class Inline() extends StaticAnnotation
final case class Dynamic() extends StaticAnnotation
final case class Hidden() extends StaticAnnotation
final case class Flag() extends StaticAnnotation
final case class Section(name: String) extends StaticAnnotation
final case class TabCompleteAsPath() extends StaticAnnotation
final case class CatchInvalidFlags() extends StaticAnnotation
final case class TabCompleteAsOneOf(options: String*) extends StaticAnnotation

final case class SectionRename(oldName: String, newName: String)(
    val conv: PartialFunction[Conf, Conf] = PartialFunction.empty,
) extends StaticAnnotation {
  require(oldName.nonEmpty && newName.nonEmpty)
  val oldNameAsSeq: Seq[String] = ArraySeq.unsafeWrapArray(oldName.split('.'))
  val newNameAsSeq: Seq[String] = ArraySeq.unsafeWrapArray(newName.split('.'))
  override def toString: String =
    s"Section '$oldName' is deprecated and renamed as '$newName'"
}
object SectionRename {
  implicit def fromTuple(obj: (String, String)): SectionRename =
    SectionRename(obj._1, obj._2)()
}
