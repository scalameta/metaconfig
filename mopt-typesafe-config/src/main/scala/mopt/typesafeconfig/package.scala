package mopt

package object typesafeconfig {
  implicit val typesafeConfigMOptParser: MOptParser =
    mopt.Hocon
}
