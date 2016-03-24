/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Jenkins LfMerge jobs
 */
import utilities.LfMerge
import utilities.common

// *********************************************************************************************
freeStyleJob('GitHub-LfMerge-Linux-any-master-release') {
	LfMerge.commonLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}')

	description '''<p>Pre-merge Linux builds of master branch. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	LfMerge.addGitHubParamAndTrigger(delegate, 'master')
}

// *********************************************************************************************
freeStyleJob('GitHub-Chorus-Linux-any-lfmerge-release') {

	description '''<p>Pre-merge Linux builds of lfmerge branch of Chorus. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	LfMerge.addGitHubParamAndTrigger(delegate, 'lfmerge')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, "sillsdev/chorus")

	steps {
		shell('''#!/bin/bash
. environ

build/buildupdate.mono.sh

xbuild /t:Test /property:BUILD_NUMBER=0.0.$BUILD_NUMBER.0 /property:Configuration=ReleaseMono build/Chorus.proj''')
	}

	publishers {
		configure common.NUnitPublisher('**/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-Chorus-Win32-lfmerge-release') {

	description '''<p>Pre-merge Windows builds of lfmerge branch of Chorus. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	LfMerge.addGitHubParamAndTrigger(delegate, 'lfmerge')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, "sillsdev/chorus")

	label 'windows'

	steps {
		batchFile('''build/buildupdate.win.sh

msbuild /t:Test /property:BUILD_NUMBER=0.0.%BUILD_NUMBER%.0 /property:Configuration=Release build/Chorus.proj''')
	}

	publishers {
		configure common.NUnitPublisher('**/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-FlexBridge-Linux-any-lfmerge-release') {

	description '''<p>Pre-merge Linux builds of lfmerge branch of FLExBridge. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	LfMerge.addGitHubParamAndTrigger(delegate, 'lfmerge')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, "sillsdev/flexbridge")

	steps {
		shell('''#!/bin/bash
. environ

./download_dependencies_linux.sh

xbuild /t:Test /property:BUILD_NUMBER=0.0.$BUILD_NUMBER.0 /property:BUILD_VCS_NUMBER=$GIT_COMMIT /property:Configuration=ReleaseMono build/FLExBridge.build.mono.proj''')
	}

	publishers {
		configure common.NUnitPublisher('**/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-FlexBridge-Win32-lfmerge-release') {

	description '''<p>Pre-merge Windows builds of lfmerge branch of FLExBridge. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	LfMerge.addGitHubParamAndTrigger(delegate, 'lfmerge')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, "sillsdev/flexbridge")

	label 'windows'

	steps {
		batchFile('''download_dependencies_windows.sh

msbuild /t:Test /property:BUILD_NUMBER=0.0.%BUILD_NUMBER%.0 /property:BUILD_VCS_NUMBER=%GIT_COMMIT% /property:Configuration=ReleaseMono build/FLExBridge.build.mono.proj''')
	}

	publishers {
		configure common.NUnitPublisher('**/TestResults.xml')
	}
}
