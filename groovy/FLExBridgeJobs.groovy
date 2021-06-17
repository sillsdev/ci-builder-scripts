/*
 * Copyright (c) 2017-2021 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'flexbridge'
def distros = 'focal bionic'
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

	upload_target = 'experimental'

	switch (branch) {
		case 'develop':
			// or 'experimental'
			upload_target = 'proposed'
			break
	}

	extraParameter = "--nightly-delimiter '~' --source-code-subdir ${subdir_name}"
	package_version = """--package-version "${fullBuildNumber}" """

	freeStyleJob("FlexBridge_Packaging-Linux-all-${branch}") {

		mainRepoDir = '.'

		Common.defaultPackagingJob(
			jobContext: delegate,
			packageName: packagename,
			subdirName: subdir_name,
			packageVersion: package_version,
			revision: revision,
			distrosToBuild: distros,
			email: email_recipients,
			branch: branch,
			archesToBuild: 'amd64',
			supportedDistros: distros,
			mainRepoDir: mainRepoDir,
			buildMasterBranch: false,
			addParameters: true,
			addSteps: false,
			extraSourceArgs: extraParameter,
			fullBuildNumber: fullBuildNumber)

		description """
<p>Automatic ("nightly") builds of the FLExBridge ${branch} branch.</p>
<p>The job is created by the DSL plugin from <i>FLExBridgeJobs.groovy</i> script.</p>
"""

		// TriggerToken needs to be set in the seed job! Note that we use the
		// `binding.variables.*` notation so that it works when we build the tests.
		authenticationToken(binding.variables.TriggerToken)

		triggers {
			githubPush()
			// Weekly on Saturday
			cron("H H * * 6")
		}

		Common.gitScm(delegate, /* url: */ repo, /* branch: */ "\$BranchOrTagToBuild",
			/* createTag: */ false, /* subdir: */ subdir_name, /* disableSubmodules: */ false,
			/* commitAuthorInChangelog: */ true, /* scmName: */ "", /* refspec: */ "",
			/* clean: */ true)

		wrappers {
			timeout {
				elastic(300, 3, 120)
				abortBuild()
				writeDescription("Build timed out after {0} minutes")
			}
			credentialsBinding {
				string('CROWDIN_API_KEY', 'FieldWorks_Crowdin_API_Key')
			}
		}

		steps {
			shell("""#!/bin/bash
export FULL_BUILD_NUMBER=${fullBuildNumber}

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	# FLEx Bridge uses "ReleaseCandidate" to mean releasing a stable when a higher-versioned Beta has already shipped
fi

cd "${subdir_name}"

make vcs_version

\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${distros}" \
	--debkeyid \$DEBSIGNKEY \
	--main-repo-dir ${mainRepoDir} \
	${package_version} \
	${extraParameter} \
	\$MAKE_SOURCE_ARGS

echo "PackageVersion=\$(dpkg-parsechangelog --show-field=Version)" >> gitversion.properties
""")

			environmentVariables {
				propertiesFile("${subdir_name}/gitversion.properties")
			}

			Common.addBuildNumber(delegate, 'PackageVersion')

			shell("""#!/bin/bash -e
if [ "\$PackageBuildKind" = "Release" ]; then
	upload_suite='main'
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	upload_suite='updates'
fi

cd "${subdir_name}"

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${distros}" \
	--suite-name "\${upload_suite}" \
	--debkeyid \$DEBSIGNKEY \
	\$BUILD_PACKAGE_ARGS
""")
		}
	}
}

// *******************************************************************************************
multibranchPipelineJob('FLExBridge') {
	description """<p>Builds of FLExBridge</p>
<p>The job is created by the DSL plugin from <i>FLExBridgeJobs.groovy</i> script.</p>"""

	branchSources {
		github {
			id('flexbridge')
			repoOwner('sillsdev')
			repository('flexbridge')
			scanCredentialsId('github-sillsdevgerrit')
			buildOriginBranch(true)
			buildOriginBranchWithPR(false)
			buildOriginPRMerge(true)
			buildForkPRMerge(true)
		}

		orphanedItemStrategy {
			discardOldItems {
				daysToKeep(60)
				numToKeep(10)
			}
		}

		triggers {
			// check once a day if not otherwise run
			periodicFolderTrigger {
				interval('1d')
			}
		}
	}
}
