package metaconfig.internal

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import metaconfig._
import org.scalameta.logger

object Macros {
  def deriveSurface[T]: Surface[T] = macro deriveSurfaceImpl[T]
  def deriveSurfaceImpl[T: c.WeakTypeTag](
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
    val objectFactory = deriveObjectFactory[T](c)(T)
    val result = q"new ${weakTypeOf[Surface[T]]}($args, $objectFactory)"
//    logger.elem(result)
    c.untypecheck(result)
//    result
  }

  def deriveObjectFactory[T: c.WeakTypeTag](c: blackbox.Context)(
      T: c.Type): c.universe.Tree = {
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
    logger.elem(result)
    result
  }

}
