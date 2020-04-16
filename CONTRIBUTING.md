# Contributing

All changes to this repo should be uploaded to Gerrit. Please see below if
you don't have an account on Gerrit yet.

A recommended setup is (replace _`<gerritusername>`_ with your gerrit account
name):

	git clone https://github.com/sillsdev/ci-builder-scripts.git
	cd ci-builder-scripts
	git config remote.origin.pushurl \
		ssh://<gerritusername>@gerrit.lsdev.sil.org:59418/ci-builder-scripts.git
	git config remote.origin.push "+refs/heads/*:refs/for/master/*"

You can push your changes to Gerrit by running:

	git push origin HEAD:refs/for/master

The FieldWorks [wiki page](https://github.com/sillsdev/FwDocumentation/wiki/Workflow-Overview)
gives an overview of the general workflow.
For the ci-builder-scripts project we use a simplified version of the
workflow which differs in so far that all development is done on the
`master` branch.

## Create new build job by adding new groovy file

To add a build job to Jenkins you can add a new groovy file. Take one of the
existing files as a template. The documentation for the Jenkins Job DSL API
can be found on the [server itself](https://jenkins.lsdev.sil.org/plugin/job-dsl/api-viewer/index.html).

You can check the syntax of the script by
running gradle (see [job-dsl-gradle-example](https://github.com/sheehan/job-dsl-gradle-example])).
There is also an [online playground](http://job-dsl.herokuapp.com/).

After committing the changes in git and uploading to Gerrit a Jenkins job
will be triggered (at the time of this writing the pre-merge build happens
only when a VM on Eberhard's desktop machine is running).

## Create account on gerrit server

If you don't have a gerrit account yet, you'll have to register with our
Gerrit [code review server](https://gerrit.lsdev.sil.org/).

Gerrit uses OpenID for authentication; you can use your existing Google or GitHub
account for that, or create an account with another OpenID provider and
enter the URL of your OpenID. If your OpenID provider supports it you
should enter an https version of the URL otherwise you might get a warning
from your browser.

Once you've registered an OpenID, you will be asked to choose a user name.
It's preferable to use firstname_lastname in all lowercase characters. You
should also set a real name, which is what other users will see in the UI.

You can also register a preferred email address (eg @sil.org) that's
different from the one that may be associated with your OpenID. If you have
problems with the link that you are sent for validating your email address,
try viewing the email in webmail since some email client programs may
mangle the long, cryptic URLs that are sent.

**After creating an account on Gerrit please send an email to _gerrit at
sil.org_ telling your username so that you can be given the correct
permissions.**
