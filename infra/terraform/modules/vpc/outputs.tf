output "vpc_id" {
  description = "ID of the VPC."
  value       = aws_vpc.this.id
}

output "vpc_cidr_block" {
  description = "Primary CIDR block of the VPC."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the private (EKS node) subnets."
  value       = aws_subnet.private[*].id
}

output "database_subnet_ids" {
  description = "IDs of the isolated database subnets."
  value       = aws_subnet.database[*].id
}

output "nat_gateway_public_ips" {
  description = "Public IPs of the NAT gateways (stable egress IPs for allowlisting)."
  value       = aws_eip.nat[*].public_ip
}

output "flow_log_group_name" {
  description = "Name of the VPC Flow Logs CloudWatch log group."
  value       = aws_cloudwatch_log_group.flow_logs.name
}
