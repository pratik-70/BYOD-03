variable "region" {
  type    = string
  default = "us-east-1"
  description = "The AWS region to deploy to."
}

variable "instance_type" {
  type    = string
  default = "t2.micro"
  description = "The EC2 instance type."
}
