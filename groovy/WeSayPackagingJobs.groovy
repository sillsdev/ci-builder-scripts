/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'WeSay'
def distros_tobuild = 'trusty xenial'
def repo = 'git://github.com/sillsdev/wesay.git'
def email_recipients = 'eb1@sil.org'

def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
def package_version = '--package-version "\${FULL_BUILD_NUMBER}" '

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

/*
 * We have two jobs on two different branches for beta/release ('master') and alpha ('develop')
 */
for (branch in ['master']) {
	switch (branch) {
		case 'master':
			subdir_name = 'wesay'
			kind = 'release'
			break
	}

	freeStyleJob("WeSay_Packaging-Linux-all-${branch}-${kind}") {

		Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
			distros_tobuild, email_recipients, branch, "amd64 i386", "trusty xenial", true, '.',
			/* buildMasterBranch: */ false)

		description """
<p>Automatic ("nightly") builds of the WeSay ${branch} branch.</p>
<p>The job is created by the DSL plugin from <i>WeSayPackagingJobs.groovy</i> script.</p>
"""

		triggers {
			githubPush()
		}

		Common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
			false, subdir_name, false, true)

		wrappers {
			timeout {
				elastic(300, 3, 90)
				abortBuild()
				writeDescription("Build timed out after {0} minutes")
			}
		}

		steps {
			shell("""#!/bin/bash
cd "${subdir_name}"
echo "PackageVersion=\$(dpkg-parsechangelog --show-field=Version)" > packageversion.properties
""")
			environmentVariables {
				propertiesFile("${subdir_name}/packageversion.properties")
			}

			Common.addBuildNumber(delegate, 'PackageVersion')
		}
	}
}
