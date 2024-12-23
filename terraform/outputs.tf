output "api_url" {
  description = "API endpoint URL"
  value       = aws_apigatewayv2_api.api.api_endpoint
}

output "lambda_log_group" {
  value = module.lambda_function.log_group_name
}
