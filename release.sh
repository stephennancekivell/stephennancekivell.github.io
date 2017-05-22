#!/bin/bash

JEKYLL_ENV=production jekyll build
aws --profile sn s3 sync ./_site s3://blog2.stephenn.com
aws --profile sn cloudfront create-invalidation --distribution-id E1DRN1L45UMGVD --paths '/*'