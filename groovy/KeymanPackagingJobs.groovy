/*
 * Copyright (c) 2018 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
//#include utilities/Common.groovy

// Variables
def distros_tobuild = 'xenial bionic'
def repo = 'git://github.com/keymanapp/keyman.git'
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
 * We are building multiple packages in this job
 */
for (packagename in ['kmflcomp', 'libkmfl', 'ibus-kmfl', 'keyman-config']) {
	subdir_name = "linux/${packagename}"
	branch = 'linux-buildsys'
	extraParameter = "--nightly-delimiter '~' --source-code-subdir ${subdir_name}"
	package_version = """--package-version "${fullBuildNumber}" """

	freeStyleJob("Keyman_Packaging-Linux-${packagename}-master") {

		mainRepoDir = '.'

		Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
			distros_tobuild, email_recipients, branch, "amd64 i386", "xenial bionic", true, mainRepoDir,
			/* buildMasterBranch: */ false, /* addParameters */ true, /* addSteps */ false,
			/* resultsDir: */ "results", /* extraSourceArgs: */ extraParameter,
			/* extraBuildArgs: */ '', /* fullBuildNumber: */ fullBuildNumber)

		description """
<p>Automatic ("nightly") builds of the Keyman for Linux master branch.</p>
<p>The job is created by the DSL plugin from <i>KeymanPackagingJobs.groovy</i> script.</p>
"""

		// TriggerToken needs to be set in the seed job! Note that we use the
		// `binding.variables.*` notation so that it works when we build the tests.
		authenticationToken(binding.variables.TriggerToken)

		triggers {
			githubPush()
		}

		Common.gitScm(delegate, /*url*/ repo, /*branch*/"\$BranchOrTagToBuild",
			/*createTag*/ false, /*subdir*/ "", /*disableSubmodules*/ false,
			/*commitAuthorInChangelog*/ true, /*scmName*/ "", /*refspec*/ "",
			/*clean*/ false, /*credentials*/ "", /*fetchTags*/ true,
			/*onlyTriggerFileSpec*/ "linux/.*",
			/*githubRepo*/ "keymanapp/keyman")

		wrappers {
			timeout {
				elastic(300, 3, 120)
				abortBuild()
				writeDescription("Build timed out after {0} minutes")
			}
		}

		steps {
			shell("""#!/bin/bash
export FULL_BUILD_NUMBER=${fullBuildNumber}

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name main"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name proposed"
fi

# make source package
cd linux
./scripts/jenkins.sh "${packagename}" "\$DEBSIGNKEY"
cd ..
cd "${subdir_name}"

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${distros_tobuild}" \
	--debkeyid \$DEBSIGNKEY \
	--build-in-place \
	\$BUILD_PACKAGE_ARGS

echo "PackageVersion=\$(dpkg-parsechangelog --show-field=Version)" > ../../${packagename}-packageversion.properties
""")

			environmentVariables {
				propertiesFile("${packagename}-packageversion.properties")
			}

			Common.addBuildNumber(delegate, 'PackageVersion')
		}
	}
}
