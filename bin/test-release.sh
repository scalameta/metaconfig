set -eux
version=$1
coursier fetch \
  com.geirsson:metaconfig-sconfig_2.12:$version \
  com.geirsson:metaconfig-sconfig_2.13:$version \
  com.geirsson:metaconfig-sconfig_2.12:$version \
  com.geirsson:metaconfig-typesafe-config_2.12:$version \
  com.geirsson:metaconfig-typesafe-config_2.13:$version \
  com.geirsson:metaconfig-typesafe-config_2.12:$version \
  -r sonatype:public
