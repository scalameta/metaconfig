package metaconfig

import scala.annotation.compileTimeOnly
import scala.collection.immutable.Map
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.tokens.Token.Constant

class Error(msg: String) extends Exception(msg)
case class FailedToReadClass(className: String, error: Throwable)
    extends Error(s"Failed to read '$className'. ${error.getMessage}")
case class ConfigError(msg: String) extends Error(msg)
case class ConfigErrors(es: scala.Seq[Throwable])
    extends Error(s"Errors: ${es.mkString("\n")}")

class Recurse extends scala.annotation.StaticAnnotation
class ExtraName(string: String) extends scala.annotation.StaticAnnotation

@compileTimeOnly("@metaconfig.Config not expanded")
class DeriveConfDecoder extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    def derviveDecoder(typ: Type, params: Seq[Term.Param] = Seq.empty): Defn.Val = {
      val mapName = Term.Name("obj")
      val classLit = Lit.String(typ.syntax)
      val extraNames: Map[String, Seq[Term.Arg]] = params.collect {
        case p: Term.Param =>
          p.name.syntax -> p.mods.collect {
            case mod"@ExtraName(..${List(extraName)})" => extraName
            case mod"@metaconfig.ExtraName(..${List(extraName)})" => extraName
          }
      }.toMap
      def defaultArgs: Seq[Term.Arg] = {
        params.collect {
          case Term.Param(mods, pName: Term.Name, Some(pTyp: Type), _) =>
            val nameLit = Lit.String(pName.syntax)
            val args = Seq(pName, nameLit) ++ extraNames(pName.syntax)
            Term.Arg.Named(
              pName,
              q"""_root_.metaconfig.Metaconfig.get[$pTyp](obj)(..$args)"""
            )
        }
      }
      val argLits =
        params.map(x => Lit.String(x.name.syntax)) ++
          extraNames.values.flatten
      val constructor = Ctor.Ref.Name(typ.syntax)
      val bind = Term.Name("x")
      val x = q"""val x = "string""""
      val patTyped = Pat.Typed(Pat.Var.Term(bind), typ.asInstanceOf[Pat.Type])
      q"""val reader: _root_.metaconfig.ConfDecoder[$typ] = new _root_.metaconfig.ConfDecoder[$typ] {
          override def read(any: _root_.metaconfig.Conf): _root_.metaconfig.Result[$typ] = {
            any match {
              case obj @ _root_.metaconfig.Conf.Obj(_) =>
                val validFields = _root_.scala.collection.immutable.Set(..$argLits)
                val invalidFields = obj.keys.filterNot(validFields)
                if (invalidFields.nonEmpty) {
                  val msg =
                    "Error reading class '" + $classLit + "'. " +
                    "Invalid fields: " + invalidFields.mkString(", ")
                  Left(_root_.metaconfig.ConfigError(msg))
                } else {
                  try {
                      Right(new $constructor(..$defaultArgs))
                  } catch {
                    case _root_.scala.util.control.NonFatal(e) =>
                      Left(_root_.metaconfig.FailedToReadClass(${typ.syntax}, e))
                  }
                }
              case els =>
                val msg =
                  $classLit + " cannot be '" + els +
                    "' (of class " + els.kind + ")."
                Left(_root_.metaconfig.ConfigError(msg))
            }
          }
        }
     """
    }

    def expandClass(c: Defn.Class): Stat = {
      val q"..$mods class $tname[..$tparams] ..$mods2 (...$paramss) extends $template" =
        c
      val template"{ ..$earlyStats } with ..$ctorcalls { $param => ..$stats }" =
        template

      // TODO(olafur) come up with a way to avoid inheritance :/
      val newCtorCalls: Seq[Ctor.Call] =
        ctorcalls :+ Ctor.Ref.Name("_root_.metaconfig.HasFields")
      val flatParams = paramss.flatten
      val fields: Seq[Term.Tuple] = flatParams.collect {
        case Term.Param(_, name: Term.Name, _, _) =>
          q"(${Lit.String(name.syntax)}, $name)"
      }
      val fieldsDef: Stat = {
        val body =
          Term.Apply(q"_root_.scala.collection.immutable.Map", fields)
        q"def fields: Map[String, Any] = $body"
      }
      val typReader = derviveDecoder(tname, flatParams)

      def implicitName(p: Term.Param): Term.Name =
        Term.Name(p.name.value + p.decltpe.map(_.syntax).getOrElse("")  + "Decoder")
      val recurses: Seq[Stat] = flatParams.collect {
        case p: Term.Param if p.mods.exists {
          case mod"@Recurse" => true
          case mod"@metaconfig.Recurse" =>  true
          case _ => false
        } =>
          val tpe = p.decltpe.get.asInstanceOf[Type]
          q"""
              implicit val ${Pat.Var.Term(implicitName(p))}: _root_.metaconfig.ConfDecoder[$tpe] =
                ${Term.Name(p.name.value)}.reader
           """
      }
      val lowPriorityImplicits = q""" object LowPriImplicits { ..$recurses } """
      val newStats =
        recurses ++
            stats ++
            Seq(fieldsDef) ++
            Seq(typReader)
      val newTemplate = template"""
        { ..$earlyStats } with ..$newCtorCalls { $param => ..$newStats }
                                  """
      val result =
        q"""
            ..$mods class $tname[..$tparams] ..$mods2 (...$paramss) extends $newTemplate
         """
      result
    }

    defn match {
      case c: Defn.Class => expandClass(c)
      case Term.Block(Seq(c: Defn.Class, companion)) =>
        q"""
        ${expandClass(c)}; $companion
         """
      case els =>
        abort(s"Failed to expand: ${els.structure}")
    }
  }
}
