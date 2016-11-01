if [ "$TRAVIS_REPO_SLUG" == "intendia-oss/autorest" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then

  mvn -s ci/settings.xml clean source:jar deploy -Dmaven.test.skip=true -Dinvoker.skip=true
fi
