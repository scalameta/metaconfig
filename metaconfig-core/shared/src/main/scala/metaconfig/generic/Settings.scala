package metaconfig.generic

import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.annotation.DeprecatedName
import metaconfig.internal.Cli

final class Settings[T](val settings: List[Setting]) {
  def fields: List[Field] = settings.map(_.field)

  @deprecated("Use flat(Conf) instead", "0.8.0")
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

  def flat(default: Conf.Obj): List[(Setting, Conf)] = {
    settings.zip(default.values).flatMap {
      case (deepSetting, (_, conf: Conf.Obj)) =>
        deepSetting.underlying.toList
          .flatMap(_.withPrefix(deepSetting.name).flat(conf))
      case (s, (_, defaultValue)) =>
        (s, defaultValue) :: Nil
    }
  }

  def withPrefix(prefix: String): Settings[T] =
    new Settings(settings.map(s => s.withName(prefix + "." + s.name)))

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
      if (setting.isDynamic) {
        Some(setting)
      } else {
        rest match {
          case Nil => Some(setting)
          case head :: tail =>
            for {
              underlying <- setting.underlying
              next <- underlying.get(head, tail)
            } yield next
        }
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
