/*
 * DSL script for Bloom Jenkins views
 */

/* Definition of views */

class bloomViews {
	static void BloomViewAll(viewContext) {
		viewContext.with {
			view('All') {
				configure { view ->
					view.name = 'org.jenkinsci.plugins.categorizedview.CategorizedJobsView'
					view / 'name' << 'All'
				}

				description 'All <b>Bloom</b> jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('(^Bloom(-|_).*|^GitHub-Bloom-.*)')
				}

				configure { view ->
					view / columns / 'hudson.views.StatusColumn'
					view / columns / 'hudson.views.WeatherColumn'
					view / columns / 'org.jenkinsci.plugins.categorizedview.IndentedJobColumn'
					view / columns / 'hudson.views.LastSuccessColumn'
					view / columns / 'hudson.views.LastFailureColumn'
					view / columns / 'hudson.views.LastDurationColumn'
					view / columns / 'hudson.views.BuildButtonColumn'
				}

				configure { view ->
					view / categorizationCriteria {
						'org.jenkinsci.plugins.categorizedview.GroupingRule' {
							groupRegex('^Bloom-.*-(default|master)-debug$')
							namingRule('master branch compile jobs')
						}
						'org.jenkinsci.plugins.categorizedview.GroupingRule' {
							groupRegex('^Bloom-.*-(default|master)-.*Tests$')
							namingRule('master branch unit tests')
						}
						'org.jenkinsci.plugins.categorizedview.GroupingRule' {
							groupRegex('.*(Packaging).*')
							namingRule('')
						}
						'org.jenkinsci.plugins.categorizedview.GroupingRule' {
							groupRegex('.*-Trigger-.*')
							namingRule('master branch builds')
						}
						'org.jenkinsci.plugins.categorizedview.GroupingRule' {
							groupRegex('^GitHub.*-master-.*')
							namingRule('Pre-merge builds of GitHub pull requests (master branch)')
						}
						'org.jenkinsci.plugins.categorizedview.GroupingRule' {
							groupRegex('^GitHub.*-Version3.0-.*')
							namingRule('Pre-merge builds of GitHub pull requests (Version3.0 branch)')
						}
					}
				}
			}
		}
	}

	static void BloomViewBuildPipeline(viewContext, viewName, branchName, selectedJobName,
		preMerge) {
		viewContext.with {
			view(viewName, type: BuildPipelineView) {

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
			view('Package builds') {
				description 'Package builds of <b>Bloom</b>'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('^Bloom_Packaging-.*')
				}

				configure { view ->
					view / columns / 'hudson.views.StatusColumn'
					view / columns / 'hudson.views.WeatherColumn'
					view / columns / 'hudson.views.JobColumn'
					view / columns / 'hudson.views.LastSuccessColumn'
					view / columns / 'hudson.views.LastFailureColumn'
					view / columns / 'hudson.views.LastDurationColumn'
					view / columns / 'hudson.views.BuildButtonColumn'
					view / columns / 'org.jenkins.plugins.builton.BuiltOnColumn'
					view / columns / 'de.fspengler.hudson.pview.ConsoleViewColumn'
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
