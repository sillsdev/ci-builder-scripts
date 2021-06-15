/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'WeSay'
def distros_tobuild = 'bionic'
def repo = 'git://github.com/sillsdev/wesay.git'
def email_recipients = 'eb1@sil.org dglassey@gmail.com'

def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
def fullBuildNumber="0.0.0+\$BUILD_NUMBER"

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

/*
 * We have multiple jobs on different branches for release, beta ('master') and alpha ('develop')
 */
for (branch in ['release/1.6', 'master', 'develop']) {
	extraParameter = ''
	switch (branch) {
		case 'release/1.6':
			packagename = 'wesay'
			kind = 'stable'
			break
		case 'master':
			packagename = 'wesay-beta'
			kind = 'beta'
			extraParameter = '--append-to-package -beta'
			break
		case 'develop':
			packagename = 'wesay-alpha'
			kind = 'alpha'
			extraParameter = '--append-to-package -alpha'
			break
	}

	subdir_name = packagename
	extraParameter = "--nightly-delimiter '~' --source-code-subdir ${subdir_name} ${extraParameter}"
	package_version = """--package-version "${fullBuildNumber}" """

	freeStyleJob("WeSay_Packaging-Linux-all-${branch.replace('/', '_').replace('-', '_')}-${kind}") {

		mainRepoDir = '.'

		Common.defaultPackagingJob(
			jobContext: delegate,
			packageName: packagename,
			subdirName: subdir_name,
			packageVersion: package_version,
			revision: revision,
			distrosToBuild: distros_tobuild,
			email: email_recipients,
			branch: branch,
			supportedDistros: 'bionic',
			mainRepoDir: mainRepoDir,
			buildMasterBranch: false,
			extraSourceArgs: extraParameter,
			fullBuildNumber: fullBuildNumber)

		description """
<p>Automatic ("nightly") builds of the WeSay ${branch} branch.</p>
<p>The job is created by the DSL plugin from <i>WeSayPackagingJobs.groovy</i> script.</p>
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
