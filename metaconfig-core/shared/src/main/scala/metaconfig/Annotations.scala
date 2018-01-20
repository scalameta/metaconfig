package metaconfig

import scala.annotation.StaticAnnotation

final case class SettingName(value: String) extends StaticAnnotation
final case class ExtraSettingName(value: String) extends StaticAnnotation
final case class DeprecatedSettingName(
    name: String,
    message: String,
    sinceVersion: String)
    extends StaticAnnotation {
  override def toString: String =
    s"Setting '$name' is deprecated since version $sinceVersion. $message"
}
final case class ExampleValue(value: String) extends StaticAnnotation
final case class SettingDescription(value: String) extends StaticAnnotation
final case class SinceVersion(value: String) extends StaticAnnotation
final case class DeprecatedSetting(message: String, since: String)
    extends StaticAnnotation
