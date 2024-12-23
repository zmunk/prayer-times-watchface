#!/usr/bin/env bash

# set -e
#     Short for `set -o errexit`
#     Exit the script immediately if any command returns a non-zero
#     (failure) exit code. This prevents the script from continuing in an
#     error state, which is particularly useful in long scripts where
#     unchecked failures might go unnoticed.
# set -u
#     Short for `set -o nounset`
#     Treat unset variables as an error, and immediately exit the script. This
#     prevents issues where a variable is mistakenly used without being defined,
#     which can otherwise lead to unpredictable behavior.
set -eu

# Echo red bold text to stderr
function eecho { echo -e "\e[1;31m$@\e[0m" 1>&2; }

usage="$0 python3.11 redis redis-py311-lambda-layer"

[ "$#" -lt 3 ] && (
  eecho "ERROR: please provide python version, library name, and layer name"
  echo "   e.g. '$usage'"
  exit 1
)

python_version="$1"
library="$2"
layer_name="$3"

cache=.aws_cache
layer_folder="$cache/lambda-layer"
site_packages="$layer_folder/python/lib/$python_version/site-packages"
zipfile=lambda-layer.zip

mkdir -p $site_packages
$python_version -m pip install $library -t $site_packages
(cd $layer_folder && zip -r ../$zipfile *)

aws lambda publish-layer-version \
    --layer-name "$layer_name" \
    --description "$library lambda layer for $python_version" \
    --license-info "MIT" \
    --zip-file "fileb://$cache/$zipfile" \
    --compatible-runtimes "$python_version" \
    --query "LayerVersionArn" \
    --output text

rm -r $layer_folder
rm $cache/$zipfile
