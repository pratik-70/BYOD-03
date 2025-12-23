terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.region # Use the variable for region
  # Credentials will be supplied via environment variables
}

// Lookup a recent Amazon Linux 2 AMI for the configured region to avoid stale AMI IDs
data "aws_ami" "amazon_linux2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

resource "aws_instance" "example" {
  ami           = data.aws_ami.amazon_linux2.id
  instance_type = var.instance_type # Use the variable for instance type

  tags = {
    Name = "ExampleInstance"
  }
}
