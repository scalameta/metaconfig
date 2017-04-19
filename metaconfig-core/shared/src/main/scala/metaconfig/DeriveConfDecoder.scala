package metaconfig

import scala.annotation.compileTimeOnly
import scala.collection.immutable.Map
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.tokens.Token.Constant

import org.scalameta.logger

class Recurse extends scala.annotation.StaticAnnotation
class ExtraName(string: String) extends scala.annotation.StaticAnnotation

@compileTimeOnly("@metaconfig.Config not expanded")
class DeriveConfDecoder extends scala.annotation.StaticAnnotation {


  inline def apply(defn: Any): Any = meta {
    def typParamToType(tparam: Type.Param): Type = {
      val name = Type.Name(tparam.name.value)
      if (tparam.tparams.isEmpty) name
      else Type.Apply(name, tparam.tparams.map(typParamToType))
    }
    def deriveDecoder(typName: Type.Name,
                      tparams: Seq[Type.Param],
                      params: Seq[Term.Param] = Seq.empty): Defn.Val = {
      val typParams = tparams.map(typParamToType)
      val typ =
        if (tparams.isEmpty) typName
        else Type.Apply(typName, typParams)
      val extraNames: Map[String, Seq[Term.Arg]] = params.collect {
        case p: Term.Param =>
          p.name.syntax -> p.mods.collect {
            case mod"@ExtraName(..${List(extraName)})" => extraName
            case mod"@metaconfig.ExtraName(..${List(extraName)})" => extraName
          }
      }.toMap
      val namesAndType = params.collect {
        case Term.Param(mods, name: Term.Name, Some(typ: Type), _) =>
          name -> typ
      }
      val names = namesAndType.map {
        case (Term.Name(name), _) =>
          Term.Name("p" + name)
      }
      val ctor = Ctor.Ref.Name(typName.value)
      val constructor =
        if (typParams.nonEmpty) q"new $ctor[..$typParams](..$names)"
        else q"new $ctor(..$names)"
      val deconstruct = Case(names.foldRight[Pat](Lit.Unit(())) {
        case (name, accum) => p"(${Pat.Var.Term(name)}, $accum)"
      }, None, constructor)
      val product =
        namesAndType.foldRight[Term](q"_root_.metaconfig.Configured.unit") {
          case ((tName @ Term.Name(name), tpe), accum) =>
            val extraName = extraNames(name)
            val get =
              q"_root_.metaconfig.Metaconfig.get[$tpe](obj)($tName, $name, ..$extraName)"
            q"$get.product($accum)"
        }
      val mapFromProduct =
        Term.Apply(q"$product.map",
                   Seq(Term.PartialFunction(Seq(deconstruct))))
      val argLits =
        params.map(x => Lit.String(x.name.syntax)) ++
          extraNames.values.flatten
      q"""val reader: _root_.metaconfig.ConfDecoder[$typ] = new _root_.metaconfig.ConfDecoder[$typ] {
          override def read(conf: _root_.metaconfig.Conf): _root_.metaconfig.Configured[$typ] = {
            conf match {
              case obj @ _root_.metaconfig.Conf.Obj(_) =>
                val validFields = _root_.scala.collection.immutable.Set(..$argLits)
                val invalidFields = obj.keys.filterNot(validFields)
                if (invalidFields.nonEmpty) {
                  _root_.metaconfig.Configured.NotOk(
                    _root_.metaconfig.ConfError.invalidFields(invalidFields, validFields))
                } else {
                  $mapFromProduct
                }
              case els =>
                _root_.metaconfig.Configured.NotOk(
                  _root_.metaconfig.ConfError.typeMismatch(${typ.syntax}, els))
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
      val typReader = deriveDecoder(tname, tparams, flatParams)

      def implicitName(p: Term.Param): Term.Name =
        Term.Name(
          p.name.value + p.decltpe.map(_.syntax).getOrElse("") + "Decoder")
      val recurses: Seq[Stat] = flatParams.collect {
        case p: Term.Param if p.mods.exists {
              case mod"@Recurse" => true
              case mod"@metaconfig.Recurse" => true
              case _ => false
            } =>
          val tpe = p.decltpe.get.asInstanceOf[Type]
          q"""
              implicit val ${Pat.Var
            .Term(implicitName(p))}: _root_.metaconfig.ConfDecoder[$tpe] =
                ${Term.Name(p.name.value)}.reader
           """
      }

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
