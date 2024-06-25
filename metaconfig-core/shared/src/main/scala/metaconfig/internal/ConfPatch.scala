package metaconfig.internal

import metaconfig.Conf

object ConfPatch {
  def patch(original: Conf, revised: Conf): Conf = (original, revised) match {
    case (Conf.Obj(a), Conf.Obj(b)) =>
      Conf.Obj(b.flatMap { case kv @ (k, v) =>
        if (a.contains(kv)) Nil
        else {
          a.find(_._1 == k) match {
            case Some((_, v1)) =>
              (k, patch(v1, v)) :: Nil
            case _ =>
              kv :: Nil
          }
        }
      })
    case (a, b) =>
      b
  }

}
