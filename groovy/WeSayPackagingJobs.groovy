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
			packagename = 'wesay'
			extraParameter = ''
			break
		case 'develop':
			subdir_name = 'wesay-alpha'
			kind = 'alpha'
			packagename = 'wesay-alpha'
			extraParameter = '--append-to-package -alpha'
			break
	}

	freeStyleJob("WeSay_Packaging-Linux-all-${branch}-${kind}") {

		Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
			distros_tobuild, email_recipients, branch, "amd64 i386", "trusty xenial", true, '.',
			/* buildMasterBranch: */ false, /* addParameters */ true, /* addSteps */ false)

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
export FULL_BUILD_NUMBER=0.0.\$BUILD_NUMBER.${revision}

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--no-upload"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--no-upload"
fi

cd "${subdir_name}"
\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${supported_distros}" \
	--debkeyid \$DEBSIGNKEY \
	--main-repo-dir ${mainRepoDir} \
	${extraParameter} \
	${package_version} \
	\$MAKE_SOURCE_ARGS

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${supported_distros}" \
	--debkeyid \$DEBSIGNKEY \
	\$BUILD_PACKAGE_ARGS

echo "PackageVersion=\$(dpkg-parsechangelog --show-field=Version)" > packageversion.properties
""")

			environmentVariables {
				propertiesFile("${subdir_name}/packageversion.properties")
			}

			Common.addBuildNumber(delegate, 'PackageVersion')
		}
	}
}
