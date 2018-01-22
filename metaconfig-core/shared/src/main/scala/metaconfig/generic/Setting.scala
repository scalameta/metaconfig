package metaconfig.generic

import scala.annotation.StaticAnnotation
import metaconfig.annotation._

final class Setting(val field: Field) {
  def name: String = field.name
  def tpe: String = field.tpe
  def annotations: List[StaticAnnotation] = field.annotations
  def underlying: List[List[Field]] = field.underlying
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
  def isBoolean: Boolean = field.tpe == "Boolean"
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
