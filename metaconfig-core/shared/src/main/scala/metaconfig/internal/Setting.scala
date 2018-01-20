package metaconfig.internal

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.ConfReads
import metaconfig.DeprecatedSetting
import metaconfig.DeprecatedSettingName
import metaconfig.ExampleValue
import metaconfig.ExtraSettingName
import metaconfig.SettingDescription
import metaconfig.SinceVersion

final class Setting(val field: Field) {
  def name: String = field.name
  def annotations: List[StaticAnnotation] = field.annotations

  def description: Option[String] = field.annotations.collectFirst {
    case SettingDescription(value) => value
  }
  def extraNames: List[String] = field.annotations.collect {
    case ExtraSettingName(value) => value
  }
  def deprecatedNames: List[DeprecatedSettingName] = field.annotations.collect {
    case d: DeprecatedSettingName => d
  }
  def exampleValues: List[String] = field.annotations.collect {
    case ExampleValue(value) => value
  }
  def sinceVersion: Option[String] = field.annotations.collectFirst {
    case SinceVersion(value) => value
  }
  def deprecated: Option[DeprecatedSetting] = field.annotations.collectFirst {
    case value: DeprecatedSetting => value
  }
  def alternativeNames: List[String] =
    extraNames ::: deprecatedNames.map(_.name)
  def allNames: List[String] = name :: alternativeNames
  def deprecation(name: String): Option[DeprecatedSettingName] =
    deprecatedNames.find(_.name == name)
  def read(conf: Conf): ConfReads.Result[Any] =
    ConfError.msg("Not implemented").result
}

object Setting {
  def apply[T: ClassTag](name: String): Setting =
    new Setting(Field(name, None, implicitly[ClassTag[T]], Nil))
  def apply[T: ClassTag: DefaultValueShow](
      name: String,
      defaultValue: T): Setting = new Setting(
    Field(name, Some(DefaultValue(defaultValue)), implicitly[ClassTag[T]], Nil)
  )
}
