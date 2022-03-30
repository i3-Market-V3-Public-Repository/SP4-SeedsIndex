#!/bin/bash

NEXUS_REGISTRY=95.211.3.251:8123
IMAGE_NAME=seedsindex-build
IMAGE_TAG=2.0.0

TAG="${NEXUS_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

docker build . --tag $TAG
docker push $TAG

