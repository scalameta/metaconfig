package metaconfig

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag

final case class SettingName(value: String) extends StaticAnnotation
final case class ExtraSettingName(value: String) extends StaticAnnotation
final case class DeprecatedSettingName(value: String) extends StaticAnnotation
final case class ExampleValue(value: String) extends StaticAnnotation
final case class SettingDescription(value: String) extends StaticAnnotation
final case class SinceVersion(value: String) extends StaticAnnotation
final case class DeprecatedSetting(message: String, since: String)
    extends StaticAnnotation

final class Setting(val field: Field) {
  def name: String = field.name
  def annotations: List[StaticAnnotation] = field.annotations

  def description: Option[String] = field.annotations.collectFirst {
    case SettingDescription(value) => value
  }
  def extraNames: List[String] = field.annotations.collect {
    case ExtraSettingName(value) => value
  }
  def deprecatedNames: List[String] = field.annotations.collect {
    case DeprecatedSettingName(value) => value
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
  def alternativeNames: List[String] = extraNames ::: deprecatedNames
  def allNames: List[String] = name :: alternativeNames
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

final class Settings[T](val settings: List[Setting]) {
  def get(name: String): Setting = settings.find(_.name == name).get
}

object Settings {
  implicit def FieldsToSettings[T](implicit ev: Fields[T]): Settings[T] =
    apply(ev)
  def apply[T](implicit ev: Fields[T]): Settings[T] =
    new Settings[T](ev.fields.map(new Setting(_)))
}
