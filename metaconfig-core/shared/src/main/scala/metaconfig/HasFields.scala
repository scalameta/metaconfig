package metaconfig

@deprecated("This is never used", "0.6.0")
trait HasFields {
  def fields: Map[String, Any]
}
