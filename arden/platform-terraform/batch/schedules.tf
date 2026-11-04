# EventBridge schedules mirroring the Autosys estate for the AWS-hosted jobs.
resource "aws_scheduler_schedule" "settle_instr_gen" {
  name                = "settle-instr-gen"
  schedule_expression = "cron(0 6 ? * MON-FRI *)" # 06:00 London; instructions for T-1 trades, T+2 cycle
  flexible_time_window { mode = "OFF" }
  target {
    arn      = var.batch_runner_arn
    role_arn = var.scheduler_role_arn
  }
}

variable "batch_runner_arn" { type = string }
variable "scheduler_role_arn" { type = string }
