/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for Jenkins LfMerge jobs
 */
import utilities.common
import utilities.LfMerge

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

// *********************************************************************************************
freeStyleJob('LfMerge-Linux-any-master-debug') {
	LfMerge.commonLfMergeBuildJob(delegate, '+refs/heads/master:refs/remotes/origin/master', '*/master')

	description '''<p>Linux builds of master branch.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	triggers {
		githubPush()
	}

	steps {
		downstreamParameterized {
			trigger('LfMerge_Packaging-Linux-all-master-release')
		}
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-LfMerge-Linux-any-master-debug') {
	LfMerge.commonLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}')

	description '''<p>Pre-merge Linux builds of master branch. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	parameters {
		stringParam("sha1", "",
			"What pull request to build, e.g. origin/pr/9/head")
	}

	triggers {
		pullRequest {
			admin('ermshiperete')
			useGitHubHooks(true)
			orgWhitelist('sillsdev')
			cron('H/5 * * * *')
			allowMembersOfWhitelistedOrgsAsAdmin()

		}
	}

	configure { project ->
		project / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'displayBuildErrorsOnDownstreamBuilds'(true)
		project / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'whiteListTargetBranches' {
			'org.jenkinsci.plugins.ghprb.GhprbBranch' { 'branch'('master') }
		}
	}
}

// *********************************************************************************************
freeStyleJob('LfMerge_InstallDependencies-Linux-any-master-debug') {
	LfMerge.generalLfMergeBuildJob(delegate, '${refspec}', '${branch}', false)

	description '''<p>Install dependency packages for LfMerge builds.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	parameters {
		stringParam("branch", "master",
			"What to build, e.g. master or origin/pr/9/head")
		stringParam("refspec", "+refs/heads/master:refs/remotes/origin/master",
			"Refspec to build")
	}

	// will be triggered by other jobs

	steps {
		// Install packages
		shell('''#!/bin/bash
set -e
PATH=/opt/mono-sil/bin:$PATH
cd build
mozroots --import --sync
./install-deps''')
	}
}

// *********************************************************************************************
freeStyleJob('LfMerge_Packaging-Linux-all-master-release') {
	def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
	def package_version = '--package-version "\${FULL_BUILD_NUMBER}" '
	def distro = 'trusty'

	steps {
		shell('''#!/bin/bash
set -e
echo "Downloading packages and dependencies"
cd lfmerge
# We need to set MONO_PREFIX because that's set to a mono 2.10 installation on the packaging machine!
export MONO_PREFIX=/opt/mono-sil
RUNMODE="PACKAGEBUILD" BUILD=Release . environ
mozroots --import --sync
yes | certmgr -ssl https://go.microsoft.com
yes | certmgr -ssl https://nugetgallery.blob.core.windows.net
yes | certmgr -ssl https://nuget.org
xbuild /t:PrepareSource build/LfMerge.proj''')
	}

	common.defaultPackagingJob(delegate, 'lfmerge', 'lfmerge', package_version, revision,
		distro, 'eb1@sil.org', 'master', 'amd64', distro, false)

	description '''
<p>Nightly builds of the LfMerge master branch.</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>
'''

	// will be triggered by other jobs

	common.gitScm(delegate, 'https://github.com/sillsdev/LfMerge.git', "\$BranchOrTagToBuild",
		false, 'lfmerge', false, true, "", "+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*",
		true)
}
