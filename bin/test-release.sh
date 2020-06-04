set -eux
version=$1
coursier fetch \
  com.geirsson:mopt-sconfig_2.12:$version \
  com.geirsson:mopt-sconfig_2.13:$version \
  com.geirsson:mopt-sconfig_2.12:$version \
  com.geirsson:mopt-typesafe-config_2.12:$version \
  com.geirsson:mopt-typesafe-config_2.13:$version \
  com.geirsson:mopt-typesafe-config_2.12:$version \
  com.geirsson:mopt-json_2.12:$version \
  com.geirsson:mopt-json_2.13:$version \
  com.geirsson:mopt-json_2.12:$version \
  -r sonatype:public
