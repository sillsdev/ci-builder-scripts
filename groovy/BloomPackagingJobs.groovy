/*
 * DSL script for Jenkins Bloom Packaging jobs
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'Bloom'
def distros_tobuild = 'trusty xenial'
def repo = 'git://github.com/BloomBooks/BloomDesktop.git'
def email_recipients = 'stephen_mcconnel@sil.org'

def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
def package_version = '--package-version "\${FULL_BUILD_NUMBER}" '

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

/*
 * We have four jobs on four different branches for alpha ('master'), beta ('Version3.9'),
 * and release ('Version3.8' and 'Version3.7').
 * Version3.7 is the last version which builds for precise, so we're keeping it around at least
 * for a bit even after precise's impending End Of Life.
 */
for (version in ['3.7', '3.8', '3.9', 'master']) {
	switch (version) {
		case '3.7':
			branch = 'Version3.7'
			subdir_name = 'bloom-desktop'
			kind = 'release'
			distros_thisjob = 'precise trusty xenial'
			break
		case '3.8':
			branch = 'Version3.8'
			subdir_name = 'bloom-desktop'
			kind = 'release'
			distros_thisjob = distros_tobuild
			break
		case '3.9':
			branch = 'Version3.9'
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
		Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
			distros_thisjob, email_recipients, branch)

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
	}
}
