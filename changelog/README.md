# Release notes

`<version>.md` in this directory is the human-written part of that release's notes:
highlights, upgrade notes, anything a reader needs that a list of merged pull requests
does not convey.

`scripts/release/create-github-release.sh` renders it at the top of the GitHub release
body, above the "Use it" snippet and GitHub's generated changelog. The stubborn.sh blog
post is generated from that release body, so whatever is written here is what the blog
post says.

Write the file in the pull request that earns it, so it is reviewed alongside the change
rather than reconstructed at release time. A release without a file here still works —
it just gets the boilerplate and the generated list of merged pull requests.
