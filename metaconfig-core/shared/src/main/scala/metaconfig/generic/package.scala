package metaconfig

import scala.language.experimental.macros

package object generic {
  def deriveSurface[T]: Surface[T] =
    macro metaconfig.internal.Macros.deriveSurfaceImpl[T]
  def deriveDecoder[T](default: T): ConfDecoder[T] =
    macro metaconfig.internal.Macros.deriveConfDecoderImpl[T]
  def deriveEncoder[T]: ConfEncoder[T] =
    macro metaconfig.internal.Macros.deriveConfEncoderImpl[T]
  def deriveCodec[T](default: T): ConfCodec[T] =
    macro metaconfig.internal.Macros.deriveConfCodecImpl[T]

  def deriveDecoderEx[T](default: T, typosAllowed: Boolean): ConfDecoderEx[T] =
    macro metaconfig.internal.Macros.deriveConfDecoderExImpl[T]
  def deriveCodecEx[T](default: T, typosAllowed: Boolean): ConfCodecEx[T] =
    macro metaconfig.internal.Macros.deriveConfCodecExImpl[T]
}
