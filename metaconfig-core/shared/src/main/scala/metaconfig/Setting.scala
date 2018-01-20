package metaconfig

import scala.annotation.StaticAnnotation

final case class SettingName(value: String) extends StaticAnnotation
final case class ExtraSettingName(value: String) extends StaticAnnotation
final case class DeprecatedSettingName(value: String) extends StaticAnnotation
final case class ExampleValue(value: String) extends StaticAnnotation
final case class SettingDescription(value: String) extends StaticAnnotation
final case class SinceVersion(value: String) extends StaticAnnotation
final case class DeprecatedSetting(warning: String, since: String)
    extends StaticAnnotation
final class Setting(
    val name: SettingName,
    val description: Option[SettingDescription],
    val extraNames: List[ExtraSettingName],
    val deprecatedNames: List[DeprecatedSettingName],
    val exampleValues: List[ExampleValue],
    val sinceVersion: Option[SinceVersion],
    val deprecated: Option[DeprecatedSetting]
) {
  def alternativeNames: List[String] =
    extraNames.map(_.value) ::: deprecatedNames.map(_.value)
  def allNames: List[String] =
    name.value :: alternativeNames
}

object Setting {
  def apply(name: String): Setting = Setting(SettingName(name))
  def apply(name: SettingName): Setting =
    new Setting(name, None, Nil, Nil, Nil,  None, None)
}

case class Settings[T](settings: List[Setting]) {
  def get(name: String): Option[Setting] = settings.find(_.name.value == name)
}

object Settings {
  def apply[T](implicit ev: Settings[T]): Settings[T] = ev
}
