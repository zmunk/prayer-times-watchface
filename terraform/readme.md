## View lambda logs
    uv run --with zmunk-awslogs -m awslogs $(tf output -raw lambda_log_group)

## Make request to server
    curl $(tf output -raw api_url)

## Get api url
    tf output -raw api_url

## Creating lambda layers
e.g. `./scripts/create-lambda-layer.sh python3.11 requests requests-py311-lambda-layer`
