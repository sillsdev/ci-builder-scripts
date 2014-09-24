/*
 * DSL script for geckofx29 Jenkins jobs
 */
import utilities.common

// Variables
def packagename = 'geckofx29';
def subdir_name = packagename;
def distros_tobuild = 'precise trusty';
def revision = "\$(echo \${MERCURIAL_REVISION_SHORT} | cut -b 1-6)";

/*
 * Definition of jobs
 */

job {
    common.defaultPackagingJob(delegate, packagename, subdir_name, "", revision, distros_tobuild);

    name 'Geckofx_NightlyPackaging-Linux-all-master-debug';

    description '''
<p>Nightly builds of the geckofx29 default branch.</p>
<p>The job is created by the DSL plugin from <i>geckofx29-jobs.groovy</i> script.</p>
''';

    common.hgScm(delegate, 'https://bitbucket.org/geckofx/geckofx-29.0', 'default', subdir_name);
    //common.hgScm(delegate, 'https://bitbucket.org/yautokes/geckofx-29.0', 'default', subdir_name);
}

