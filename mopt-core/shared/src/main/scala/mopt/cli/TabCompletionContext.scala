package mopt.cli

import mopt.generic.Setting

case class TabCompletionContext(
    format: Option[String],
    current: Option[Int],
    arguments: List[String],
    last: String,
    secondLast: Option[String],
    setting: Option[Setting],
    allSettings: Map[String, Setting],
    app: CliApp
)
