/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

// *********************************************************************************************
multibranchPipelineJob('icu-dotnet') {
	description '''<p>Builds of icu-dotnet</p>
<p>The job is created by the DSL plugin from <i>IcudotnetJobs.groovy</i> script.</p>'''

	branchSources {
		github {
			repoOwner('sillsdev')
			repository('icu-dotnet')
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
	}
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
	}
}