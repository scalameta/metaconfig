package metaconfig.internal

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import metaconfig._
import org.scalameta.logger

object Macros {
  def deriveSurface[T]: Surface[T] = macro Macros.deriveSurfaceImpl[T]
  def deriveConfDecoder[T](default: T): ConfDecoder[T] =
    macro Macros.deriveConfDecoderImpl[T]
}

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
    logger.elem(result)
    result
  }

  def deriveSurfaceImpl[T: c.WeakTypeTag]: Tree = {
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    val Tclass = T.typeSymbol.asClass
    val none = typeOf[None.type].termSymbol
    val some = typeOf[Some[_]].typeSymbol
    val ctor = Tclass.primaryConstructor.asMethod
    val fields = for {
      (params, i) <- ctor.paramLists.zipWithIndex
      (param, j) <- params.zipWithIndex
    } yield {
      val default = if (i == 0 && param.asTerm.isParamWithDefault) {
        val nme = TermName(termNames.CONSTRUCTOR + "$default$" + (j + 1)).encodedName.toTermName
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
    val objectFactory = deriveObjectFactory[T](T)
    val result = q"new ${weakTypeOf[Surface[T]]}($args, $objectFactory)"
//    logger.elem(result)
    c.untypecheck(result)
//    result
  }

  def deriveObjectFactory[T: c.WeakTypeTag](T: Type): Tree = {
    import c.universe._
    val ctor = T.typeSymbol.asClass.primaryConstructor
    val Tname = T.typeSymbol.name.decodedName.toString

    val casts = ctor.asMethod.paramLists.zipWithIndex.map {
      case (params, i) =>
        params.zipWithIndex.map {
          case (param, j) =>
            val tpe = param.info.resultType
            val expectedType = tpe.toString
            val field = Tname + "." + param.name.decodedName.toString
            val value = q"argss($i)($j)"
            q"_root_.metaconfig.ObjectFactory.cast[$tpe]($field, $expectedType, $value)"
        }
    }

    val result = q"""
    new ${weakTypeOf[ObjectFactory[T]]} {
      override def unsafeNewInstance(
        argss: _root_.scala.List[_root_.scala.List[_root_.scala.Any]]
      ): $T = {
        new ${T.typeSymbol}(...$casts)
      }
    }
     """
//    logger.elem(result)
    result
  }

}
