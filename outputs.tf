output "instance_public_ip" {
  value = aws_instance.example.public_ip
  description = "The public IP address of the EC2 instance."
}

output "instance_id" {
  value = aws_instance.example.id
  description = "The instance ID of the EC2 instance."
}

output "ssh_private_key" {
  value = tls_private_key.deployer.private_key_pem
  description = "The SSH private key for connecting to the instance."
  sensitive = true
}
