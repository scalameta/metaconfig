package metaconfig.internal

import metaconfig.DeprecatedSettingName

final class Settings[T](val settings: List[Setting]) {
  object Deprecated {
    def unapply(key: String): Option[DeprecatedSettingName] =
      (for {
        setting <- settings
        deprecation <- setting.deprecation(key).toList
      } yield deprecation).headOption
  }
  def allNames: List[String] =
    for {
      setting <- settings
      name <- setting.allNames
    } yield name
  def getOption(name: String): Option[Setting] =
    settings.find(_.allNames.contains(name))
  def get(name: String): Setting = getOption(name).get
}

object Settings {
  implicit def FieldsToSettings[T](implicit ev: Fields[T]): Settings[T] =
    apply(ev)
  def apply[T](implicit ev: Fields[T]): Settings[T] =
    new Settings[T](ev.fields.map(new Setting(_)))
}