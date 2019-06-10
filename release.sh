#!/bin/bash

aws --profile sn cloudfront create-invalidation --distribution-id EZBKNZ3I0201S --paths '/*'
