/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'flexbridge'
def distros = 'xenial trusty'
def repo = 'git://github.com/sillsdev/flexbridge.git'
def email_recipients = 'eb1@sil.org'
def subdir_name = 'flexbridge'

def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
def fullBuildNumber="0.0.0+\$BUILD_NUMBER"

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

for (branch in ['develop']) {
	/*
	switch (branch) {
		case 'develop':
			subdir_name = 'flexbridge'
			break
	}
	*/

	extraParameter = "--nightly-delimiter '~' --source-code-subdir ${subdir_name}"
	package_version = """--package-version "${fullBuildNumber}" """

	freeStyleJob("FlexBridge_Packaging-Linux-all-${branch}") {

		mainRepoDir = '.'

		Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
			distros, email_recipients, branch, "amd64 i386", distros, true, mainRepoDir,
			/* buildMasterBranch: */ false, /* addParameters */ true, /* addSteps */ true,
			/* resultsDir: */ "results", /* extraSourceArgs: */ extraParameter,
			/* extraBuildArgs: */ '', /* fullBuildNumber: */ fullBuildNumber)

		description """
<p>Automatic ("nightly") builds of the FLExBridge ${branch} branch.</p>
<p>The job is created by the DSL plugin from <i>FLExBridgeJobs.groovy</i> script.</p>
"""

		// TriggerToken needs to be set in the seed job! Note that we use the
		// `binding.variables.*` notation so that it works when we build the tests.
		authenticationToken(binding.variables.TriggerToken)

		triggers {
			githubPush()
		}

		Common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
			false, subdir_name, false, true)

		wrappers {
			timeout {
				elastic(300, 3, 120)
				abortBuild()
				writeDescription("Build timed out after {0} minutes")
			}
		}

		steps {
			shell("""#!/bin/bash
cd "${subdir_name}"
echo "PackageVersion=\$(dpkg-parsechangelog --show-field=Version)" > ../${packagename}-packageversion.properties
""")

			environmentVariables {
				propertiesFile("${packagename}-packageversion.properties")
			}

			Common.addBuildNumber(delegate, 'PackageVersion')
		}
	}
}
