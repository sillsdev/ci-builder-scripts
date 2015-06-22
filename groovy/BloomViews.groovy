/*
 * DSL script for Bloom Jenkins views
 */

/* Definition of views */

class bloomViews {
	static void BloomViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All <b>Bloom</b> jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('(^Bloom(-|_).*|^GitHub-Bloom-.*)')
				}

				columns {
					status()
					weather()
					categorizedJob()
					lastSuccess()
					lastFailure()
					lastDuration()
					buildButton()
				}

				categorizationCriteria {
					regexGroupingRule('^Bloom-.*-(default|master)-debug$', 'master branch compile jobs')
					regexGroupingRule('^Bloom-.*-(default|master)-.*Tests$', 'master branch unit tests')
					regexGroupingRule('.*(Packaging).*', '')
					regexGroupingRule('.*-Trigger-.*', 'master branch builds')
					regexGroupingRule('^GitHub.*-master-.*', 'Pre-merge builds of GitHub pull requests (master branch)')
					regexGroupingRule('^GitHub.*-Version3.0-.*', 'Pre-merge builds of GitHub pull requests (Version3.0 branch)')
				}
			}
		}
	}

	static void BloomViewBuildPipeline(viewContext, viewName, branchName, selectedJobName,
		preMerge) {
		viewContext.with {
			buildPipelineView(viewName) {

				title "Builds of the `$branchName` branch"
				if (preMerge)
					description "Pre-merge builds of the $branchName branch of <b>Bloom</b>"
				else
					description "<b>Bloom</b> builds of the $branchName branch"

				filterBuildQueue false
				filterExecutors false

				displayedBuilds 10
				refreshFrequency 3

				triggerOnlyLatestJob false
				alwaysAllowManualTrigger true
				showPipelineDefinitionHeader false
				showPipelineParameters false
				showPipelineParametersInHeaders true
				consoleOutputLinkStyle OutputStyle.Lightbox

				selectedJob selectedJobName
			}
		}
	}

	static void BloomViewPackageBuilds(viewContext) {
		viewContext.with {
			listView('Package builds') {
				description 'Package builds of <b>Bloom</b>'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('^Bloom_Packaging-.*')
				}

				columns {
					status()
					weather()
					name()
					lastSuccess()
					lastFailure()
					lastDuration()
					buildButton()
					lastBuildNode()
					lastBuildConsole()
				}
				configure { view ->
					view / columns / 'hudson.plugins.nodenamecolumn.NodeNameColumn'
				}
			}
		}
	}
}

nestedView('Bloom') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		bloomViews.BloomViewAll(delegate)
		bloomViews.BloomViewBuildPipeline(delegate, "Build Pipeline master branch",
			"master", 'Bloom-Wrapper-Trigger-debug', false)
		bloomViews.BloomViewBuildPipeline(delegate, "PR pipeline Version3.0",
			"Version3.0", 'GitHub-Bloom-Wrapper-Version3.0-debug', true)
		bloomViews.BloomViewBuildPipeline(delegate, "PR pipeline master branch",
			"master", 'GitHub-Bloom-Wrapper-master-debug', true)
		bloomViews.BloomViewPackageBuilds(delegate)
	}
}
