/*
 * DSL script for Jenkins Bloom Packaging jobs
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'Bloom'
def distros_tobuild = 'focal bionic'
def repo = 'https://github.com/BloomBooks/BloomDesktop.git'
def email_recipients = 'stephen_mcconnel@sil.org'
def packagingAgent = 'packager'

def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
def package_version = '--package-version "\${FULL_BUILD_NUMBER}" '

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

/*
 * We have up to four jobs (for four different branches) for alpha ('master'), betainternal (eg, 'Version4.3'),
 * beta (eg, 'Version4.2'), and release (eg, 'Version4.1').
 * betainternal is used periodically when a new release is almost ready and we are in the process
 * of shifting the previous alpha to beta.  Except for alpha on the master branch, the other jobs all
 * shift branches as new releases are made, with a new job created for betainternal (or beta if we skip
 * that step).
 */
for (version in ['5.1', '5.2', 'master']) {
	switch (version) {
		case '5.1':
			branch = 'Version5.1'
			subdir_name = 'bloom-desktop'
			kind = 'release'
			distros_thisjob = distros_tobuild
			break
		case '5.2':
			branch = 'Version5.2'
			subdir_name = 'bloom-desktop-beta'
			kind = 'beta'
			distros_thisjob = distros_tobuild
			break
		case 'master':
			branch = 'master'
			subdir_name = 'bloom-desktop-alpha'
			kind = 'alpha'
			distros_thisjob = distros_tobuild
			break
	}

	freeStyleJob("Bloom_Packaging-Linux-all-${version}-${kind}") {

		Common.defaultPackagingJob(
			jobContext: delegate,
			packageName: packagename,
			subdirName: subdir_name,
			packageVersion: package_version,
			revision: revision,
			distrosToBuild: distros_thisjob,
			email: email_recipients,
			branch: branch,
			archesToBuild: 'amd64',
			supportedDistros: distros_thisjob,
			buildMasterBranch: false,
			fullBuildNumber: "0.0.\$BUILD_NUMBER.${revision}",
			nodeLabel: packagingAgent,
			addSteps: false)

		description """
<p>Automatic ("nightly") builds of the Bloom ${branch} branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
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
export FULL_BUILD_NUMBER="0.0.\$BUILD_NUMBER.${revision}"

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name main"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name proposed"
fi

rm -f results/*
rm -f ${subdir_name}_*

cd "${subdir_name}"

echo "export FULL_BUILD_NUMBER=\$FULL_BUILD_NUMBER" > build_number.env

\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${distros_thisjob}" \
	--debkeyid \$DEBSIGNKEY \
	--main-repo-dir '.' \
	${package_version} \
	\$MAKE_SOURCE_ARGS

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packagename}" \
	--supported-distros "${distros_thisjob}" \
	--debkeyid \$DEBSIGNKEY \
	\$BUILD_PACKAGE_ARGS

RESULT=\$?
cd \$WORKSPACE
mv ${subdir_name}_* results/

exit \$RESULT""")
		}

	}
}
