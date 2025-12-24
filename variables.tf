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

variable "key_name" {
  type    = string
  default = ""
  description = "The name of the key pair to use for EC2 instance. Leave empty to skip."
}
