package metaconfig.internal

import metaconfig.Conf

object ConfPatch {

  // compact patch so that merge(conf, patch) == merge(srcConf, compact)
  def compact(conf: Conf, extra: Conf): Conf = (conf, extra) match {
    case (a: Conf.Obj, b: Conf.Obj) => b.flatMap { case kv @ (k, vB) =>
        a.field(k) match {
          case Some(vA) => if (vA eq vB) None else Some(k -> compact(vA, vB))
          case _ => Some(kv)
        }
      }
    case (_, b) => b
  }

}
