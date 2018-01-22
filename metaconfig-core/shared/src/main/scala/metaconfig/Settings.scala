package metaconfig

final class Settings[T](val settings: List[Setting]) {
  override def toString: String = s"Surface(settings=$settings)"
  object Deprecated {
    def unapply(key: String): Option[DeprecatedName] =
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
  implicit def FieldsToSettings[T](implicit ev: Surface[T]): Settings[T] =
    apply(ev)
  def apply[T](implicit ev: Surface[T]): Settings[T] =
    new Settings[T](ev.fields.flatten.map(new Setting(_)))
}
