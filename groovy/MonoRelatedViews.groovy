/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Mono Jenkins views
 */

/* Definition of views */

class monoRelatedViews {
	static void MonoViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All <b>Mono</b> jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('^(Gerrit-)?(Mono|Gtksharp|Libgdiplus|Monobasic)(_Nightly|-).*')
				}

				columns {
					status()
					weather()
					categorizedJob()
					lastSuccess()
					lastFailure()
					lastDuration()
					buildButton()
					lastBuildNode()
					lastBuildConsole()
					slaveOrLabel()
				}

				categorizationCriteria {
					regexGroupingRule('^(Mono|Gtk|Libgdiplus).*-any-develop-debug$', 'develop branch jobs (Mono 4.4)')
					regexGroupingRule('^(Mono|Gtk|Libgdiplus).*-any-release_mono_sil_3.4-debug$', 'release/mono-sil-3.4 branch jobs (Mono 3.4)')
					regexGroupingRule('.*(Packaging).*-develop-.*', 'Package builds of develop branch (Mono 4.4)')
					regexGroupingRule('.*(Packaging).*-release_mono_sil_3.4-.*', 'Package builds of release/mono-sil-3.4 branch (Mono 3.4)')
					regexGroupingRule('^Gerrit-(Mono|Gtk|Libgdiplus).*-develop-.*', 'Gerrit builds for Mono 4.4 (develop branch)')
					regexGroupingRule('^Gerrit-(Mono|Gtk|Libgdiplus).*-release_mono_sil_3.4-.*', 'Gerrit builds for Mono 3.4 (release/mono-sil-3.4 branch)')
				}
			}
		}
	}

	static void MonoViewPackageBuilds(viewContext) {
		viewContext.with {
			categorizedJobsView('Package builds') {
				description 'Package builds of custom <b>Mono</b>'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('^(Mono|Gtk|Libgdiplus).*Packaging-.*')
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
					slaveOrLabel()
				}

				categorizationCriteria {
					regexGroupingRule('.*-develop-.*', 'Package builds of develop branch (Mono 4.4)')
					regexGroupingRule('.*-release_mono_sil_3.4-.*', 'Package builds of release/mono-sil-3.4 branch (Mono 3.4)')
				}
			}
		}
	}
}
nestedView('Mono') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		monoRelatedViews.MonoViewAll(delegate)
		monoRelatedViews.MonoViewPackageBuilds(delegate)
	}
}
