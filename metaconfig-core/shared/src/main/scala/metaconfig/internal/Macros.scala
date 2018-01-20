package metaconfig.internal

import scala.language.experimental.macros

import scala.reflect.macros.blackbox
import metaconfig._
import org.scalameta.logger

object Macros {
  def deriveDecoder[T]: ConfDecoder[T] = macro deriveDecoderImpl[T]
  def deriveDecoderImpl[T: c.WeakTypeTag](
      c: blackbox.Context): c.universe.Tree = {
    import c.universe._
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    val conf = Ident(TermName(c.freshName("conf")))
    val settings = Ident(TermName(c.freshName("settings")))
    val field :: fields = T.members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        q"$conf.get[${m.info}]($settings.get(${m.name.decodedName.toString}))"
    }
    val product = fields.foldLeft(field) { case (a, b) => q"$b.product($a)" }
    val settingsT = weakTypeOf[Settings[T]]

    val result = q"""
       new ${weakTypeOf[ConfDecoder[T]]} {
         def read($conf: ${typeOf[Conf]}): ${weakTypeOf[Configured[T]]} = {
           val $settings = _root_.scala.Predef.implicitly[$settingsT]
           $product
         }
       }
     """
    logger.elem(result)

    q"???"
  }

  def deriveSettings[T]: Settings[T] = macro deriveSettingsImpl[T]
  def deriveSettingsImpl[T: c.WeakTypeTag](
      c: blackbox.Context): c.universe.Tree = {
    import c.universe._
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass)
      c.abort(c.enclosingPosition, s"$T must be a class")
    val ctor =
      T.member(termNames.CONSTRUCTOR).asMethod.paramLists.flatten
    val none: Tree = Ident(typeOf[None.type].termSymbol)
    val nil: Tree = Ident(typeOf[Nil.type].termSymbol)

    def joinList(lst: Iterable[Tree]): Tree =
      lst.foldLeft(nil) { case (a, b) => q"$b :: $a" }

    def getAnnotations(name: Name): List[Annotation] =
      ctor.find(_.name == name).get.annotations

    val settings = T.members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        val annots = getAnnotations(m.name)

        def get[A: TypeTag]: List[Tree] = annots.collect {
          case annot if annot.tree.tpe <:< typeOf[A] =>
            annot.tree
        }

        def option[A: TypeTag]: Tree =
          get[A].headOption.fold(none)(value =>
            q"new _root_.scala.Some($value)")
        def list[A: TypeTag] = joinList(get[A])

        val name = Literal(Constant(m.name.decodedName.toString))
        q"""new ${typeOf[Setting]}(
              name = new ${typeOf[SettingName]}($name),
              description = ${option[SettingDescription]},
              extraNames = ${list[ExtraSettingName]},
              deprecatedNames = ${list[DeprecatedSettingName]},
              exampleValues = ${list[ExampleValue]},
              sinceVersion = ${option[SinceVersion]},
              deprecated = ${option[DeprecatedSetting]}
           )"""
    }
    val result =
      q"new _root_.metaconfig.Settings[${T.typeSymbol}](${joinList(settings)})"
    c.untypecheck(result)
  }

}
