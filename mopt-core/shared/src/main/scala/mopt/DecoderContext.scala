package mopt

final class DecoderContext private (
    val conf: Conf
)

object DecoderContext {
  def apply(conf: Conf): DecoderContext = {
    new DecoderContext(conf)
  }
}
