package metaconfig.internal

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import metaconfig._
import org.scalameta.logger

object Macros {
  def deriveFields[T]: Fields[T] = macro deriveFieldsImpl[T]
  def deriveFieldsImpl[T: c.WeakTypeTag](
      c: blackbox.Context): c.universe.Tree = {
    import c.universe._
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    val Tclass = T.typeSymbol.asClass
    val none = typeOf[None.type].termSymbol
    val some = typeOf[Some[_]].typeSymbol
    val ctor = Tclass.primaryConstructor.asMethod
    val fields = for {
      (params, i) <- ctor.paramLists.zipWithIndex
      param <- params
    } yield {
      val default = if (i == 0 && param.asTerm.isParamWithDefault) {
        val nme = TermName(termNames.CONSTRUCTOR + "$default$" + (i + 1)).encodedName.toTermName
        val getter = T.companion.member(nme)
        val defaultValue = q"_root_.metaconfig.DefaultValue($getter)"
        q"new $some($defaultValue)"
      } else q"$none"
      val annots = param.annotations.collect {
        case annot if annot.tree.tpe <:< typeOf[StaticAnnotation] =>
          annot.tree
      }
      val paramTpe = internal.typeRef(
        NoPrefix,
        typeOf[ClassTag[_]].typeSymbol,
        param.info :: Nil
      )

      val classtag = c.inferImplicitValue(paramTpe)
      val field = q"""new _root_.metaconfig.Field(
           ${param.name.decodedName.toString},
           $default,
           $classtag,
           _root_.scala.List.apply(..$annots)
         )"""
      field
    }
    val args = q"_root_.scala.List.apply(..$fields)"
    val result = q"new ${weakTypeOf[Fields[T]]}($args)"
    logger.elem(result)
    c.untypecheck(result)
//    result
  }

  def deriveDecoder[T]: ConfDecoder[T] = macro deriveDecoderImpl[T]
  def deriveDecoderImpl[T: c.WeakTypeTag](
      c: blackbox.Context): c.universe.Tree = {
    import c.universe._
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    val conf = Ident(TermName(c.freshName("conf")))
    internal.setType(conf, typeOf[Conf])
    val arg = Ident(TermName(c.freshName("arg")))
    val settings = Ident(TermName(c.freshName("settings")))
    val fields = T.members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        val tpe = m.info.resultType
        tpe -> q"conf.get[$tpe]($settings.get(${m.name.decodedName.toString}))"
    }.toList
    val joined = q"_root_.scala.List.apply(..${fields.map {
      case (_, tree) => tree
    }})"
    val results =
      q"_root_.metaconfig.Configured.traverse[${typeOf[Any]}]($joined)"
    val args = fields.reverse.zipWithIndex.map {
      case ((tpe, _), i) => q"result.apply($i).asInstanceOf[$tpe]"
    }
    val ctor = q"new ${weakTypeOf[T]}(..$args)"
    val settingsT = weakTypeOf[Settings[T]]

    val result = q"""
       new ${weakTypeOf[ConfDecoder[T]]} {
         def read(conf: _root_.metaconfig.Conf): ${weakTypeOf[Configured[T]]} = {
           val $settings = _root_.scala.Predef.implicitly[$settingsT]
           val results: _root_.metaconfig.Configured[List[Any]] = $results
           results.map { result =>
             $ctor
           }
         }
       }
     """
    c.untypecheck(result)
  }

}
