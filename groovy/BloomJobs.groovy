/*
 * DSL script for Bloom Jenkins jobs
 */
import utilities.common

// Variables
def packagename = 'Bloom';
def subdir_name = 'bloom-desktop';
def distros_tobuild = 'precise trusty';

/*
 * Definition of jobs
 */

job {
    common.defaultPackagingJob(delegate, packagename, subdir_name, distros_tobuild);

    name 'Bloom_NightlyPackaging-Linux-all-master-debug';

    description '''
<p>Nightly builds of the Bloom default branch.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
''';

    common.hgScm(delegate, 'https://bitbucket.org/hatton/bloom-desktop', 'default', subdir_name);
}

