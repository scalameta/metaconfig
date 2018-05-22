package metaconfig.generic

import metaconfig.ConfEncoder
import metaconfig.annotation.DeprecatedName
import metaconfig.internal.Cli

final class Settings[T](val settings: List[Setting]) {
  def fields: List[Field] = settings.map(_.field)
  def flat(default: T)(implicit ev: T <:< Product): List[(Setting, Any)] = {
    settings
      .zip(default.productIterator.toIterable)
      .flatMap {
        case (deepSetting, defaultSetting: Product) =>
          deepSetting.flat.zip(defaultSetting.productIterator.toIterable)
        case (s, defaultValue) =>
          (s, defaultValue) :: Nil
      }

  }
  override def toString: String = s"Surface(settings=$settings)"
  object Deprecated {
    def unapply(key: String): Option[DeprecatedName] =
      (for {
        setting <- settings
        deprecation <- setting.deprecation(key).toList
      } yield deprecation).headOption
  }
  def names: List[String] = settings.map(_.name)
  def allNames: List[String] =
    for {
      setting <- settings
      name <- setting.allNames
    } yield name

  def get(name: String): Option[Setting] =
    settings.find(_.matchesLowercase(name))

  def get(name: String, rest: List[String]): Option[Setting] =
    get(name).flatMap { setting =>
      rest match {
        case Nil => Some(setting)
        case _ :: Nil if setting.isMap => Some(setting)
        case head :: tail =>
          for {
            underlying <- setting.underlying
            next <- underlying.get(head, tail)
          } yield next
      }
    }
  def unsafeGet(name: String): Setting = get(name).get
  @deprecated("Use ConfEncoder[T].write instead", "0.8.1")
  def withDefault(default: T)(
      implicit ev: T <:< Product): List[(Setting, Any)] =
    settings.zip(default.productIterator.toList)

  def toCliHelp(default: T)(implicit ev: ConfEncoder[T]): String =
    toCliHelp(default, 80)
  def toCliHelp(default: T, width: Int)(implicit ev: ConfEncoder[T]): String =
    Cli.help[T](default)(ev, this).render(width)
}

object Settings {
  implicit def FieldsToSettings[T](implicit ev: Surface[T]): Settings[T] =
    apply(ev)
  def apply[T](implicit ev: Surface[T]): Settings[T] =
    new Settings[T](ev.fields.flatten.map(new Setting(_)))
}
