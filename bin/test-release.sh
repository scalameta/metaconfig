set -eux
version=$1
coursier fetch \
  org.scalameta:metaconfig-sconfig_2.12:$version \
  org.scalameta:metaconfig-sconfig_2.13:$version \
  org.scalameta:metaconfig-sconfig_2.12:$version \
  org.scalameta:metaconfig-typesafe-config_2.12:$version \
  org.scalameta:metaconfig-typesafe-config_2.13:$version \
  org.scalameta:metaconfig-typesafe-config_2.12:$version \
  -r sonatype:public
