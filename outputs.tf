output "instance_public_ip" {
  value = aws_instance.example.public_ip
  description = "The public IP address of the EC2 instance."
}

output "instance_id" {
  value = aws_instance.example.id
  description = "The instance ID of the EC2 instance."
}
