
#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_08308bef445d_key -iv $encrypted_08308bef445d_iv -in .travis/codesigning.asc.enc -out .travis/codesigning.asc -d

    gpg --fast-import .travis/codesigning.asc
fi
