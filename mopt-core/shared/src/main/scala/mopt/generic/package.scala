package mopt

import scala.language.experimental.macros

package object generic {
  def deriveSurface[T]: Surface[T] =
    macro mopt.internal.Macros.deriveSurfaceImpl[T]
  def deriveDecoder[T](default: T): ConfDecoder[T] =
    macro mopt.internal.Macros.deriveConfDecoderImpl[T]
  def deriveEncoder[T]: ConfEncoder[T] =
    macro mopt.internal.Macros.deriveConfEncoderImpl[T]
  def deriveCodec[T](default: T): ConfCodec[T] =
    macro mopt.internal.Macros.deriveConfCodecImpl[T]
}
