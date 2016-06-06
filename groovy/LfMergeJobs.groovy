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

def distro = 'trusty'

// *********************************************************************************************
freeStyleJob('LfMerge-Linux-any-master-release') {
	LfMerge.commonLfMergeBuildJob(delegate, '+refs/heads/master:refs/remotes/origin/master', '*/master', true, true)

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
freeStyleJob('LfMerge-Linux-any-live-release') {
	LfMerge.commonLfMergeBuildJob(delegate, '+refs/heads/live:refs/remotes/origin/live', '*/live', true, true)

	description '''<p>Linux builds of live branch.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	triggers {
		githubPush()
	}

	steps {
		downstreamParameterized {
			trigger('LfMerge_Packaging-Linux-all-live-release')
		}
	}
}

// *********************************************************************************************
freeStyleJob('LfMerge_InstallDependencies-Linux-any-master-release') {
	LfMerge.generalLfMergeBuildJob(delegate, '${refspec}', '${branch}', false, false)

	description '''<p>Install dependency packages for LfMerge builds.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	parameters {
		stringParam("branch", "master",
			"What to build, e.g. master or origin/pr/9/head")
		stringParam("refspec", "refs/heads/master",
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

	// Last step: deploy lfmerge package to TeamCity build agent. 2016-05 RM
	steps {
		shell('''#!/bin/bash
echo Waiting 5 minutes for package to show up on LLSO
sleep 300
ssh ba-trusty64weba sudo apt-get update || true
ssh ba-trusty64weba sudo apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install lfmerge -y || true''')
	}
}

// *********************************************************************************************
freeStyleJob('LfMerge_Packaging-Linux-all-live-release') {
	def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
	def package_version = '--package-version "\${FULL_BUILD_NUMBER}" '

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
		distro, 'eb1@sil.org', 'live', 'amd64', distro, false, '.', false)

	description '''
<p>Release builds of the LfMerge live branch.</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>
'''

	// will be triggered by other jobs

	common.gitScm(delegate, 'https://github.com/sillsdev/LfMerge.git', "\$BranchOrTagToBuild",
		false, 'lfmerge', false, true, "", "+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*",
		true)
}

// *********************************************************************************************
freeStyleJob('LfMergeFDO_Packaging-Linux-all-lfmerge-release') {
	def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
	def package_version = '--package-version "0.0.0.\${BUILD_NUMBER}" '
	def fwBranch = 'feature/lfmerge'
	def debianBranch = 'feature/lfmerge'
	def libcomBranch = 'develop'

	description '''
<p>Package builds of the <b>lfmerge-fdo</b> package.</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>
'''

	multiscm {
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/libcom')
				refspec("refs/heads/${libcomBranch}")
			}
			branch libcomBranch
			extensions {
				relativeTargetDirectory('lfmerge-fdo/libcom')
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
				}
			}
		}
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/FwDebian')
				refspec("refs/heads/${debianBranch}")
			}
			branch debianBranch
			extensions {
				relativeTargetDirectory('lfmerge-fdo/debian')
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
				}
			}
		}
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/FieldWorks')
				refspec("refs/heads/${fwBranch}")
			}
			branch fwBranch
			extensions {
				relativeTargetDirectory('lfmerge-fdo/fw')
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
				}
			}
		}
	}

	triggers {
		gerrit {
			events {
				refUpdated()
				changeMerged()
			}
			project('FieldWorks', "ant:*${fwBranch}")
			project('FwDebian', "ant:*${debianBranch}")
			project('libcom', "ant:*${libcomBranch}")
		}
	}

	environmentVariables(DistributionsToPackage: distro, ArchesToPackage: 'amd64')

	common.defaultPackagingJob(delegate, 'lfmerge-fdo', 'lfmerge-fdo', package_version, revision,
		distro, 'eb1@sil.org', 'master', '', '', false, 'fw', false, false)

}
