package metaconfig.internal

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import metaconfig._
import metaconfig.generic.Field
import metaconfig.generic.Settings
import metaconfig.generic.Surface

object Macros
class Macros(val c: blackbox.Context) {
  import c.universe._
  def assumeClass[T: c.WeakTypeTag]: Type = {
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    T
  }
  def deriveConfDecoderImpl[T: c.WeakTypeTag](default: Tree): Tree = {
    val T = assumeClass[T]
    val Tclass = T.typeSymbol.asClass
    val settings = c.inferImplicitValue(weakTypeOf[Settings[T]])
    if (settings == null || settings.isEmpty) {
      c.abort(
        c.enclosingPosition,
        s"Missing implicit for ${weakTypeOf[Settings[T]]}]. " +
          s"Hint, add `implicit val surface: ${weakTypeOf[Surface[T]]}` " +
          s"to the companion ${T.companion.typeSymbol}"
      )
    }
    val paramss = Tclass.primaryConstructor.asMethod.paramLists
    if (paramss.size > 1) {
      c.abort(
        c.enclosingPosition,
        s"Curried parameter lists are not supported yet."
      )
    }

    val (head :: params) :: Nil = paramss
    def next(param: Symbol): Tree = {
      val P = param.info.resultType
      val name = param.name.decodedName.toString
      val getter = T.member(param.name)
      val fallback = q"tmp.$getter"
      val next = q"conf.getSettingOrElse[$P](settings.get($name), $fallback)"
      next
    }
    val product = params.foldLeft(next(head)) {
      case (accum, param) => q"$accum.product(${next(param)})"
    }
    val tupleExtract = 1.to(params.length).foldLeft(q"t": Tree) {
      case (accum, _) => q"$accum._1"
    }
    var curr = tupleExtract
    val args = 0.to(params.length).map { _ =>
      val old = curr
      curr = curr match {
        case q"$qual._1" =>
          q"$qual._2"
        case q"$qual._1._2" =>
          q"$qual._2"
        case q"$qual._2" =>
          q"$qual"
        case q"t" => q"t"
      }
      old
    }
    val ctor = q"new $T(..$args)"

    val result = q"""
       new ${weakTypeOf[ConfDecoder[T]]} {
         def read(conf: _root_.metaconfig.Conf): ${weakTypeOf[Configured[T]]} = {
             val settings = $settings
             val tmp = $default
             $product.map { t =>
               $ctor
             }
         }
       }
     """
    result
  }

  def deriveSurfaceImpl[T: c.WeakTypeTag]: Tree = {
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    val Tclass = T.typeSymbol.asClass
    val ctor = Tclass.primaryConstructor.asMethod
    val argss = ctor.paramLists.map { params =>
      val fields = params.map { param =>
        val paramTpe = param.info.resultType
        val annots = param.annotations.collect {
          case annot if annot.tree.tpe <:< typeOf[StaticAnnotation] =>
            annot.tree
        }
        val fieldsParamTpe = c.internal.typeRef(
          NoPrefix,
          weakTypeOf[Surface[_]].typeSymbol,
          paramTpe :: Nil
        )
        val underlyingInferred = c.inferImplicitValue(fieldsParamTpe)
        val underlying =
          if (underlyingInferred == null || underlyingInferred.isEmpty) {
            q"_root_.scala.Nil"
          } else {
            q"$underlyingInferred.fields"
          }

        val field = q"""new ${weakTypeOf[Field]}(
           ${param.name.decodedName.toString},
           ${paramTpe.toString},
           _root_.scala.List.apply(..$annots),
           $underlying
         )"""
        field
      }
      val args = q"_root_.scala.List.apply(..$fields)"
      args
    }
    val result = q"new ${weakTypeOf[Surface[T]]}(..$argss)"
    c.untypecheck(result)
  }

}
