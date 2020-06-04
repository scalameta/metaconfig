package metaconfig

package object typesafeconfig {
  implicit val typesafeConfigMetaconfigParser: MetaconfigParser =
    metaconfig.Hocon
}
