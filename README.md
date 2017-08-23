# Jenkins Job Definitions

This repo contains definitions of our Jenkins jobs (consisting of groovy
scripts) as well as shell scripts used in these jobs. Additional shell
scripts can be found in the [FwSupportTools](https://github.com/sillsdev/FwSupportTools) repo.

(see [job-dsl-gradle-example](https://github.com/sheehan/job-dsl-gradle-example)
for setup of gradle)

## File Structure

    .
    ├── bash                    # Shell script files
    ├── groovy                  # DSL script files (job definitions)
    ├── src
    │   ├── main
    │   │   ├── groovy          # support classes
    │   │   └── resources
    │   │       └── idea.gdsl   # IDE support for IDEA
    │   └── test
    │       └── groovy          # specs
    ├── Tests                   # NUnit tests
    └── build.gradle            # build file
