package metaconfig

import scala.annotation.StaticAnnotation

final case class SettingName(value: String) extends StaticAnnotation
final case class ExampleValue(value: String) extends StaticAnnotation
final case class SettingDescription(value: String) extends StaticAnnotation
final case class SinceVersion(value: String) extends StaticAnnotation
final case class Setting(
    name: SettingName,
    extraNames: List[SettingName] = Nil,
    deprecatedNames: List[SettingName] = Nil,
    exampleValues: List[ExampleValue] = Nil,
    settingDescription: Option[SettingDescription] = None,
    sinceVersion: Option[SinceVersion] = None,
    isDeprecated: Boolean = false
) {
  def alternativeNames: List[String] =
    extraNames.map(_.value) ::: deprecatedNames.map(_.value)
  def allNames: List[String] =
    name.value :: alternativeNames
}
