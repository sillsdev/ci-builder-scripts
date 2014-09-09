/*
 * DSL script for Bloom Jenkins jobs
 */
import utilities.Helper

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
	--supported-distros "@@{supported_distros}" \
	--debkeyid $DEBSIGNKEY

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
    name 'Bloom_NightlyPackaging-Linux-all-master-debug';

    description '''
<p>Nightly builds of the Bloom default branch.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
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
