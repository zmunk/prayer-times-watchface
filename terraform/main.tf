terraform {
  required_version = ">= 1.2.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.32"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

module "lambda_function" {
  source  = "zmunk/lambda/aws"
  version = "~> 1.0.0"

  function_name = "prayer-times"
  description   = "Returns prayer times for Uskudar"
  handler       = "lambda_function.lambda_handler"
  runtime       = "python3.11"
  source_path   = "lambda"

  layers = [
    "arn:aws:lambda:us-east-1:654654424659:layer:requests-py311-lambda-layer:1",
  ]
}
resource "aws_lambda_permission" "apigw" {
  action        = "lambda:InvokeFunction"
  function_name = module.lambda_function.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.api.execution_arn}/*/*"
}
resource "aws_apigatewayv2_api" "api" {
  name          = "api"
  protocol_type = "HTTP"
  target        = module.lambda_function.arn
}

