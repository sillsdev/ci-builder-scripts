/*
 * DSL script for Bloom Jenkins jobs
 */
import groovy.text.*;

public class Helper {
    def static prepareScript(String script, Map binding) {
        def templateEngine = new SimpleTemplateEngine();
        def template = templateEngine.createTemplate(
            script.replaceAll(~/\$/, /\\\$/).replaceAll(~/@@/, /\$/));

        return template.make(binding).toString();
    }
}

// Variables
def packagename = 'Bloom';
def supported_distros = 'precise trusty utopic wheezy saucy';
def distros_tobuild = 'precise trusty';
def subdir_name = 'bloom-desktop';
def arches_tobuild = "amd64 i386";

/*
 * Definition of build step scripts
 */

def build_script = '''
#!/bin/bash
cd "@@{subdir_name}"
$HOME/ci-builder-scripts/bash/make-source --dists "$DistributionsToPackage" \
	--arches "$ArchesToPackage" \
	--main-package-name "@@{packagename}" \
	--supported-distros "@@{supported_distros}"

$HOME/ci-builder-scripts/bash/build-package --dists "$DistributionsToPackage" \
	--arches "$ArchesToPackage" \
	--main-package-name "@@{packagename}" \
	--supported-distros "@@{supported_distros}" \
    --debkeyid $DEBSIGNKEY
'''

/*
 * Definition of jobs
 */

job {
    name 'FwSupportTools-Linux-packager-debug-checkoutonly'

    description '''
This job keeps the FwSupportTools directory up-to-date on the packaging machine.

The job is created by the DSL plugin from BloomJobs.groovy script.
'''

    label 'packager';

    customWorkspace("$HOME/FwSupportTools")

    scm {
        git("https://github.com/sillsdev/FwSupportTools", "develop")
    }

    triggers {
        // Check every hour for new changes
        // REVIEW: might be more efficient to use the Github Push Notification trigger
        scm("H * * * *");
    }
}

job {
    name 'Bloom_NightlyPackaging-Linux-all-master-debug';

    description '''
Nightly builds of the Bloom default branch.

The job is created by the DSL plugin from BloomJobs.groovy script.
''';

    label 'packager';

    parameters {
        stringParam("DistributionsToPackage", distros_tobuild,
            "The distributions to build packages for");
        stringParam("ArchesToPackage", arches_tobuild,
            "The architectures to build packages for");
    }

    scm {
        hg('https://bitbucket.org/hatton/bloom-desktop', 'default') { node ->
            node / clean('true');
            node / subdir(subdir_name);
        }
    }

    triggers {
        // Check every day for new changes
        scm("H H * * *");
    }

    wrappers {
        timestamps();
    }

    steps {
        def values = [ 'packagename' : packagename,
            'supported_distros' : supported_distros,
            'subdir_name' : subdir_name ];

        shell(Helper.prepareScript(build_script, values));
     }
}
