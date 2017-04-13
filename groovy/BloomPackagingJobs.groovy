/*
 * DSL script for Jenkins Bloom Packaging jobs
 */
//#include utilities/Common.groovy

// Variables
def packagename = 'Bloom'
def subdir_name = 'bloom-desktop'
def subdir_name_beta = 'bloom-desktop-beta'
def subdir_name_alpha = 'bloom-desktop-alpha'
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
freeStyleJob('Bloom_Packaging-Linux-all-3.7-release') {
	Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
		'precise trusty xenial', email_recipients, 'Version3.7')

	description '''
<p>Automatic ("nightly") builds of the Bloom Version3.7 branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
'''

	triggers {
		githubPush()
	}

	Common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
		false, subdir_name, false, true)

	wrappers {
		timeout {
			likelyStuck()
			abortBuild()
			writeDescription("Build timed out after {0} minutes")
		}
	}
}

freeStyleJob('Bloom_Packaging-Linux-all-3.8-release') {
	Common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
		distros_tobuild, email_recipients, 'Version3.8')

	description '''
<p>Automatic ("nightly") builds of the Bloom Version3.8 branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
'''

	triggers {
		githubPush()
	}

	Common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
		false, subdir_name_beta, false, true)

	wrappers {
		timeout {
			likelyStuck()
			abortBuild()
			writeDescription("Build timed out after {0} minutes")
		}
	}
}

freeStyleJob('Bloom_Packaging-Linux-all-3.9-beta') {
	Common.defaultPackagingJob(delegate, packagename, subdir_name_beta, package_version, revision,
		distros_tobuild, email_recipients, 'Version3.9')

	description '''
<p>Automatic ("nightly") builds of the Bloom Version3.9 branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
'''

	triggers {
		githubPush()
	}

	Common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
		false, subdir_name_beta, false, true)

	wrappers {
		timeout {
			likelyStuck()
			abortBuild()
			writeDescription("Build timed out after {0} minutes")
		}
	}
}

freeStyleJob('Bloom_Packaging-Linux-all-master-alpha') {
	Common.defaultPackagingJob(delegate, packagename, subdir_name_alpha, package_version, revision,
		distros_tobuild, email_recipients, 'master')

	description '''
<p>Nightly builds of the Bloom master branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
'''

	triggers {
		githubPush()
	}

	Common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
		false, subdir_name_alpha, false, true)

	wrappers {
		timeout {
			likelyStuck()
			abortBuild()
			writeDescription("Build timed out after {0} minutes")
		}
	}
}
