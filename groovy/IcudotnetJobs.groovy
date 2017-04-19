/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Jenkins icu-dotnet jobs
 */
//#include utilities/Common.groovy
//#include utilities/IcuDotNet.groovy

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

// *********************************************************************************************
freeStyleJob('IcuDotNet-Linux-any-master-release') {

	description '''<p>Linux builds of master branch.</p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	IcuDotNet.addGitHubPushTrigger(delegate)
	IcuDotNet.commonLinuxBuildJob(delegate)
}

// *********************************************************************************************
freeStyleJob('IcuDotNet-Win-any-master-release') {

	description '''<p>Windows builds of master branch.</p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	IcuDotNet.addGitHubPushTrigger(delegate)
	IcuDotNet.commonWindowsBuildJob(delegate)
}

// *********************************************************************************************
freeStyleJob('GitHub-IcuDotNet-Linux-any-master-release') {

	description '''<p>Pre-merge Linux builds of master branch.</p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'master', 'linux', 'conniey')
	IcuDotNet.commonLinuxBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}')
}

// *********************************************************************************************
freeStyleJob('GitHub-IcuDotNet-Win-any-master-release') {

	description '''<p>Pre-merge Windows builds of master branch.</p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'master', 'windows', 'conniey')
	IcuDotNet.commonWindowsBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', true)
}

// *********************************************************************************************
multibranchPipelineJob('icu4c') {
	description '''<p>Builds of ICU4C nuget packages</p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	branchSources {
		github {
			repoOwner('sillsdev')
			repository('icu4c')
			scanCredentialsId('github-sillsdevgerrit')
			excludes('tags/*')
			buildOriginBranch(true)
			buildOriginBranchWithPR(false)
			buildOriginPRMerge(true)
			buildForkPRMerge(true)
		}

		orphanedItemStrategy {
			discardOldItems {
				numToKeep(10)
			}
		}

		triggers {
			// run once a day if not otherwise run
			periodic(1440)
		}
	}
}