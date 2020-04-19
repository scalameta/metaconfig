case class RemoveUnusedConfig(
    @Description("Remove unused imports")
    imports: Boolean = true,
    @Description("Remove unused private members")
    privates: Boolean = true,
    @Description("Remove unused local definitions")
    locals: Boolean = true
)
