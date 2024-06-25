package metaconfig.generic

import scala.annotation.StaticAnnotation
import metaconfig.annotation._
import metaconfig.internal.CliParser

final class Setting(val field: Field) {
  def name: String = field.name
  def withName(name: String): Setting = new Setting(field.withName(name))
  def tpe: String = field.tpe
  def annotations: List[StaticAnnotation] = field.annotations
  def underlying: Option[Settings[Nothing]] =
    if (field.underlying.isEmpty) None
    else {
      Some(
        new Settings(field.underlying.flatten.map(new Setting(_)))
      )
    }
  def flat: List[Setting] =
    field.flat.map(new Setting(_))
  override def toString: String = s"Setting($field)"

  def description: Option[String] = field.annotations.collectFirst {
    case Description(value) => value
  }
  def extraNames: List[String] = field.annotations.collect {
    case ExtraName(value) => value
  }
  def deprecatedNames: List[DeprecatedName] = field.annotations.collect {
    case d: DeprecatedName => d
  }
  def exampleValues: List[String] = field.annotations.collect {
    case ExampleValue(value) => value
  }
  def sinceVersion: Option[String] = field.annotations.collectFirst {
    case SinceVersion(value) => value
  }
  def deprecated: Option[Deprecated] = field.annotations.collectFirst {
    case value: Deprecated => value
  }
  def isRepeated: Boolean =
    field.annotations.exists(_.isInstanceOf[Repeated])
  def isDynamic: Boolean =
    annotations.exists(_.isInstanceOf[Dynamic])
  def isHidden: Boolean =
    annotations.exists(_.isInstanceOf[Hidden])
  def isBoolean: Boolean =
    annotations.exists(_.isInstanceOf[Flag])
  def isTabCompleteAsPath: Boolean =
    annotations.exists(_.isInstanceOf[TabCompleteAsPath])
  def isCatchInvalidFlags: Boolean =
    annotations.exists(_.isInstanceOf[CatchInvalidFlags])
  def isPositionalArgument: Boolean =
    annotations.exists {
      case ExampleValue(CliParser.PositionalArgument) => true
      case _ => false
    }
  def tabCompleteOneOf: Option[List[String]] =
    annotations.collectFirst { case oneof: TabCompleteAsOneOf =>
      oneof.options.toList
    }
  @deprecated("Use isDynamic instead", "0.8.2")
  def isMap: Boolean = field.tpe.startsWith("Map")
  @deprecated("Use isDynamic instead", "0.8.2")
  def isConf: Boolean = field.tpe.contains("Conf")
  def alternativeNames: List[String] =
    extraNames ::: deprecatedNames.map(_.name)
  def allNames: List[String] = name :: alternativeNames
  def matchesLowercase(name: String): Boolean =
    allNames.exists(_.equalsIgnoreCase(name))
  def deprecation(name: String): Option[DeprecatedName] =
    deprecatedNames.find(_.name == name)
}

object Setting {
  def apply[T](name: String, tpe: String): Setting =
    new Setting(new Field(name, tpe, Nil, Nil))
}
