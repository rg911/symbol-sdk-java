#!/usr/bin/env bash
set -e

if [ "$TRAVIS_BRANCH" = "$RELEASE_BRANCH" ]; then

  REMOTE_NAME="origin"

  git remote rm $REMOTE_NAME

  echo "Setting remote url https://github.com/${TRAVIS_REPO_SLUG}.git"
  git remote add $REMOTE_NAME "https://${GITHUB_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git" > /dev/null 2>&1

  echo "Checking out $RELEASE_BRANCH as travis leaves the head detached."
  git checkout $RELEASE_BRANCH

  echo "Current Version"
  cat version.txt
  echo ""

  echo "Testing git remote"
  git branch -vv

#  echo "Pushing test message to $REMOTE_NAME $POST_RELEASE_BRANCH"
#  git commit --allow-empty -m "Testing credentials"
#  git push --set-upstream $REMOTE_NAME $RELEASE_BRANCH:$POST_RELEASE_BRANCH

  if [ "$SKIP_RELEASE_PUBLISH" = "true" ]; then
    echo "Skipping publishing of sdk artifacts"
    ./gradlew release
    echo ""
  else
    echo "Releasing sdk jars and javadocs"
    ./gradlew release publish gitPublishPush
    ./gradlew closeAndReleaseRepository
    echo ""
  fi

  echo "New Version"
  cat version.txt
  echo ""

  echo "Pushing code to $REMOTE_NAME $POST_RELEASE_BRANCH"
  git push --set-upstream $REMOTE_NAME $RELEASE_BRANCH:$POST_RELEASE_BRANCH
  echo "Pushing tags to $REMOTE_NAME"
  git push --tags $REMOTE_NAME
else
  echo "Release is disabled"
fi
