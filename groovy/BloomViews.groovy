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
					lastBuildTriggerColumn {
						causeDisplayType("icon")
					}
					buildButton()
				}

				categorizationCriteria {
					regexGroupingRule('.*(Packaging).*', '')
				}
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
					lastBuildTriggerColumn {
						causeDisplayType("icon")
					}
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
		bloomViews.BloomViewPackageBuilds(delegate)
	}
}
