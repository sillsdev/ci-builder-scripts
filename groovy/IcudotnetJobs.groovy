/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Jenkins icu-dotnet jobs
 */
import utilities.IcuDotNet
import utilities.common

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

// *********************************************************************************************
freeStyleJob('IcuDotNet-Linux-any-master-release') {

	description '''<p>Linux builds of master branch.<p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	IcuDotNet.addGitHubPushTrigger(delegate)
	IcuDotNet.commonLinuxBuildJob(delegate)
}

// *********************************************************************************************
freeStyleJob('IcuDotNet-Win-any-master-release') {

	description '''<p>Windows builds of master branch.<p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	IcuDotNet.addGitHubPushTrigger(delegate)
	IcuDotNet.commonWindowsBuildJob(delegate)
}

// *********************************************************************************************
freeStyleJob('GitHub-IcuDotNet-Linux-any-master-release') {

	description '''<p>Pre-merge Linux builds of master branch.<p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	common.addGitHubParamAndTrigger(delegate, 'master', 'linux')
	IcuDotNet.commonLinuxBuildJob(delegate)
}

// *********************************************************************************************
freeStyleJob('GitHub-IcuDotNet-Win-any-master-release') {

	description '''<p>Pre-merge Windows builds of master branch.<p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	common.addGitHubParamAndTrigger(delegate, 'master', 'windows')
	IcuDotNet.commonWindowsBuildJob(delegate)
}

